package s06_error_handling.err05_custom_exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 3–6: mapowanie wyjątków na ProblemDetail w jednym advice. {@code @WebMvcTest} + atrapa serwisu
 * ({@code @MockitoBean}) — atrapa może rzucić KAŻDY wyjątek, więc sprawdzamy dokładnie warstwę web.
 */
@WebMvcTest(OrderController.class)
class Err05CustomExceptionsWebTest {

    private static final String VALID_BODY = "{\"customer\":\"Ala\",\"productId\":1,\"quantity\":2}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    private ListAppender<ILoggingEvent> logs;   // appender = odbiorca logów; ListAppender zbiera je w liście
    private Logger adviceLogger;

    @BeforeEach
    void captureLogs() {
        adviceLogger = (Logger) LoggerFactory.getLogger(ShopExceptionAdvice.class);
        logs = new ListAppender<>();
        logs.start();
        adviceLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        adviceLogger.detachAppender(logs);
    }

    private ResultActions postOrder(String json) throws Exception {
        return mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    @DisplayName("3. ProductNotFoundException → 404 problem+json z type, title, detail i kodem domenowym")
    void notFoundMapping() throws Exception {
        given(orderService.place(any())).willThrow(new ProductNotFoundException(999));

        postOrder(VALID_BODY)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://shop.example.com/problems/product-not-found"))
                .andExpect(jsonPath("$.title").value("Produkt nie istnieje"))
                .andExpect(jsonPath("$.detail").value("Nie znaleziono produktu o id 999"))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("3. OutOfStockException → 409 z dodatkowymi polami requested i available")
    void outOfStockMapping() throws Exception {
        given(orderService.place(any())).willThrow(new OutOfStockException(2, 3, 0));

        postOrder(VALID_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.requested").value(3))
                .andExpect(jsonPath("$.available").value(0));
    }

    @Test
    @DisplayName("4. Walidacja @Valid → 400 i mapa błędów: pole → lista komunikatów (posortowana)")
    void validationErrors() throws Exception {
        postOrder("{\"customer\":\"\",\"productId\":-1,\"quantity\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://shop.example.com/problems/validation-failed"))
                .andExpect(jsonPath("$.errors.customer[0]").value("Podaj nazwę klienta"))
                .andExpect(jsonPath("$.errors.productId[0]").value("Id produktu musi być dodatnie"))
                .andExpect(jsonPath("$.errors.quantity[0]").value("Minimalna ilość to 1"));
    }

    @Test
    @DisplayName("4. Uszkodzony JSON → 400 problem+json (odziedziczone po ResponseEntityExceptionHandler), 405 dla złej metody")
    void springMvcErrorsKeepTheirCodes() throws Exception {
        postOrder("{\"customer\":")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/orders"))   // adres przyjmuje tylko POST; catch-all Exception NIE przerobił 405 na 500
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("5. Błąd 5xx: klient nie widzi przyczyny technicznej, a log dostaje DOKŁADNIE JEDEN wpis ERROR z przyczyną")
    void serverErrorLoggedOnceAndHidden() throws Exception {
        given(orderService.place(any()))
                .willThrow(new PaymentGatewayException(new IllegalStateException("Connection refused: pay.internal:8443")));

        String body = postOrder(VALID_BODY)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PAYMENT_UNAVAILABLE"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("pay.internal", "Connection refused");
        assertThat(logs.list).filteredOn(event -> event.getLevel() == Level.ERROR).hasSize(1);
        ILoggingEvent event = logs.list.get(0);
        assertThat(event.getThrowableProxy().getCause().getMessage()).contains("pay.internal");   // szczegóły są W LOGU
    }

    @Test
    @DisplayName("5. Błąd klienta (4xx) NIE trafia do logu jako ERROR — to nie awaria serwera")
    void clientErrorsAreNotLoggedAsErrors() throws Exception {
        given(orderService.place(any())).willThrow(new ProductNotFoundException(999));

        postOrder(VALID_BODY).andExpect(status().isNotFound());
        postOrder("{\"customer\":\"\",\"productId\":1,\"quantity\":1}").andExpect(status().isBadRequest());

        assertThat(logs.list).isEmpty();
    }

    @Test
    @DisplayName("6. Nieprzewidziany wyjątek → 500 ze stałym komunikatem; szczegóły (tabela ORDERS_V2) tylko w logu, raz")
    void unexpectedExceptionIsGeneric() throws Exception {
        given(orderService.place(any())).willThrow(new IllegalStateException("Niespójny stan tabeli ORDERS_V2"));

        String body = postOrder(VALID_BODY)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Wystąpił nieoczekiwany błąd"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("ORDERS_V2");
        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.get(0).getThrowableProxy().getMessage()).contains("ORDERS_V2");
    }
}
