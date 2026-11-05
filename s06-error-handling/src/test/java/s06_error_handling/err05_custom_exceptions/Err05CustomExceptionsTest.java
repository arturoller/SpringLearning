package s06_error_handling.err05_custom_exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 2, 3 i 5 lekcji Err05CustomExceptions. {@code @WebMvcTest(ShopController.class)} ładuje kontroler i
 * {@code Err05Advice}, a serwis jest zastąpiony atrapą ({@code @MockitoBean} — mock = atrapa obiektu) rzucającą wyjątki
 * domenowe. Dzięki temu testujemy TYLKO mapowanie wyjątek → odpowiedź HTTP, bez logiki sklepu.
 */
@WebMvcTest(ShopController.class)
class Err05CustomExceptionsTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService service;

    private ResultActions expectProblem(ResultActions result, int status, String code) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    @DisplayName("Sekcja 2: tabela mapowania — NotFoundException → 404, OutOfStock i OrderAlreadyShipped → 409, MinimumOrderValue → 422")
    void statusTable() throws Exception {
        given(service.getProduct(9)).willThrow(new ProductNotFoundException(9));
        given(service.getOrder(7)).willThrow(new OrderNotFoundException(7));
        given(service.placeOrder(1, 9)).willThrow(new OutOfStockException("Kawa", 9, 5));
        given(service.ship(2)).willThrow(new OrderAlreadyShippedException(2));
        given(service.placeOrder(1, 1)).willThrow(new MinimumOrderValueException(2));

        expectProblem(mockMvc.perform(get("/err05/products/9")), 404, "PRODUCT_NOT_FOUND");
        expectProblem(mockMvc.perform(get("/err05/orders/7")), 404, "ORDER_NOT_FOUND");
        expectProblem(mockMvc.perform(post("/err05/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":9}")), 409, "OUT_OF_STOCK");
        expectProblem(mockMvc.perform(post("/err05/orders/2/ship")), 409, "ORDER_ALREADY_SHIPPED");
        expectProblem(mockMvc.perform(post("/err05/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":1}")), 422, "MINIMUM_ORDER_VALUE");
    }

    @Test
    @DisplayName("Sekcja 3: kod biznesowy trafia do właściwości code i do adresu type; klient nie musi parsować tekstu")
    void businessCodeInProblemDetail() throws Exception {
        given(service.placeOrder(1, 9)).willThrow(new OutOfStockException("Kawa", 9, 5));

        mockMvc.perform(post("/err05/orders").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":9}"))
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/out-of-stock"))
                .andExpect(jsonPath("$.title").value("Konflikt ze stanem sklepu"))
                .andExpect(jsonPath("$.detail").value("Brak towaru: Kawa (chcesz 9, jest 5)"))
                .andExpect(jsonPath("$.instance").value("/err05/orders"));
    }

    @Test
    @DisplayName("Sekcja 2: nowy wyjątek w gałęzi NotFoundException dostaje 404 bez zmiany advice (najbliższy typ w hierarchii)")
    void newSubclassNeedsNoNewHandler() throws Exception {
        class CustomerNotFoundException extends NotFoundException {   // klasa lokalna: powstaje tylko na potrzeby testu
            CustomerNotFoundException() {
                super("CUSTOMER_NOT_FOUND", "Nie ma klienta");
            }
        }
        given(service.getOrder(5)).willThrow(new CustomerNotFoundException());

        expectProblem(mockMvc.perform(get("/err05/orders/5")), 404, "CUSTOMER_NOT_FOUND")
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/customer-not-found"));
    }

    @Test
    @DisplayName("Sekcja 5: nieoczekiwany wyjątek → 500, kod UNEXPECTED, bez tajnych szczegółów")
    void unexpectedException() throws Exception {
        expectProblem(mockMvc.perform(get("/err05/crash")), 500, "UNEXPECTED")
                .andExpect(jsonPath("$.detail").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(content().string(not(containsString("hasło"))));
    }

    @Test
    @DisplayName("Sekcja 5: pytanie 'czy istnieje?' zwraca 200 z true/false (Optional), a nie 404")
    void existsUsesOptional() throws Exception {
        given(service.findProduct(1)).willReturn(java.util.Optional.of(new Product(1, "Kawa", 5)));
        given(service.findProduct(9)).willReturn(java.util.Optional.empty());

        mockMvc.perform(get("/err05/products/1/exists")).andExpect(status().isOk()).andExpect(jsonPath("$.exists").value(true));
        mockMvc.perform(get("/err05/products/9/exists")).andExpect(status().isOk()).andExpect(jsonPath("$.exists").value(false));
    }

    @Test
    @DisplayName("Sekcja 3: wyjątki Springa (walidacja, zła metoda) nadal obsługuje klasa bazowa — też jako ProblemDetail")
    void springExceptionsStillWork() throws Exception {
        mockMvc.perform(post("/err05/orders").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/err05/products/1"))
                .andExpect(status().isMethodNotAllowed());
    }
}
