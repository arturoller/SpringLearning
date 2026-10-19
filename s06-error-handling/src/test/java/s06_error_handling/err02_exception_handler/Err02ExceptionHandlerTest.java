package s06_error_handling.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Err02ExceptionHandler. {@code @WebMvcTest} (wycinek warstwy webowej: tylko kontrolery i MVC, bez reszty
 * aplikacji) + MockMvc wystarcza, bo handlery budują odpowiedź SAME — ścieżka przez /error nie jest potrzebna.
 * Ciało odpowiedzi z /error (dla @ResponseStatus i ResponseStatusException) sprawdza Err02DefaultJsonTest na RANDOM_PORT.
 */
@WebMvcTest
class Err02ExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    HandlerExceptionResolver handlerExceptionResolver;   // bean „handlerExceptionResolver”: złożony z kilku rozwiązywaczy

    @Test
    @DisplayName("1. @ResponseStatus na wyjątku: ProductNotFoundException → 404")
    void responseStatusOnException() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products/XXX")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/ELE-001/offer")).andExpect(status().isGone());
    }

    @Test
    @DisplayName("2. ResponseStatusException niesie status w sobie: 409 Conflict")
    void responseStatusException() throws Exception {
        mockMvc.perform(get("/api/products/ELE-001/reserve")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("3. @ExceptionHandler w kontrolerze buduje własne ciało błędu (402 z kodem PLATNOSC)")
    void localHandlerBuildsBody() throws Exception {
        mockMvc.perform(post("/api/payments/other"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PLATNOSC"))
                .andExpect(jsonPath("$.message").value("Płatność nie powiodła się"));
    }

    @Test
    @DisplayName("4. Wygrywa najbardziej szczegółowy handler: CardDeclinedException → KARTA_ODRZUCONA, nie PLATNOSC")
    void mostSpecificHandlerWins() throws Exception {
        mockMvc.perform(post("/api/payments/card"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("KARTA_ODRZUCONA"));
    }

    @Test
    @DisplayName("5. Handler na wyjątek Javy (IllegalArgumentException) → 400 z własnym kodem")
    void handlerForJavaException() throws Exception {
        mockMvc.perform(post("/api/payments/bug"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ZLY_ARGUMENT"));
    }

    @Test
    @DisplayName("6. Handler z PaymentController nie działa w OrderController — wyjątek nieobsłużony")
    void handlerIsLocalToItsController() {
        // W MockMvc nieobsłużony wyjątek leci do testu (patrz Err01MockMvcTest); w prawdziwej aplikacji to 500
        assertThatThrownBy(() -> mockMvc.perform(post("/api/orders/ZAM-001/pay")))
                .hasCauseInstanceOf(PaymentException.class);
    }

    @Test
    @DisplayName("7. @ExceptionHandler wygrywa z @ResponseStatus na klasie wyjątku: kupon → 409, nie 410")
    void handlerBeatsResponseStatus() throws Exception {
        mockMvc.perform(get("/api/coupons/ABC"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("KUPON_WYGASL"));
    }

    @Test
    @DisplayName("8. Dwa handlery na ten sam wyjątek w jednej klasie → IllegalStateException „Ambiguous @ExceptionHandler”")
    void ambiguousHandlers() {
        assertThatThrownBy(() -> new ExceptionHandlerMethodResolver(AmbiguousController.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ambiguous @ExceptionHandler method mapped for [ExceptionHandler{exceptionType=java.lang.IllegalStateException");
    }

    @Test
    @DisplayName("9. PUŁAPKA: handler bez @ResponseStatus i bez ResponseEntity odpowiada 200 OK, choć w ciele jest błąd")
    void handlerWithoutStatusGives200() throws Exception {
        mockMvc.perform(post("/api/payments/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NIEOBSLUGIWANE"));
    }

    @Test
    @DisplayName("10. Kolejność rozwiązywaczy wyjątków: najpierw @ExceptionHandler, potem @ResponseStatus, na końcu domyślne")
    void resolversOrder() {
        var composite = (HandlerExceptionResolverComposite) handlerExceptionResolver;

        assertThat(composite.getExceptionResolvers()).extracting(r -> r.getClass().getSimpleName())
                .containsSubsequence("ExceptionHandlerExceptionResolver", "ResponseStatusExceptionResolver",
                        "DefaultHandlerExceptionResolver");
    }

    @Test
    @DisplayName("8b. Błąd „Ambiguous” wychodzi dopiero przy PIERWSZYM wyjątku z tego kontrolera, nie przy starcie")
    void ambiguousHandlersFailLazily() throws Exception {
        MockMvc standalone = MockMvcBuilders.standaloneSetup(new AmbiguousController()).build();   // start się udaje

        assertThatThrownBy(() -> standalone.perform(get("/boom")))
                .hasStackTraceContaining("Ambiguous @ExceptionHandler method mapped for");
    }

    /** Klasa celowo błędna: dwa handlery dla IllegalStateException. Nie jest beanem — służy tylko do sprawdzenia mechanizmu. */
    @RestController
    static class AmbiguousController {
        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("bum");
        }

        @ExceptionHandler(IllegalStateException.class)
        String first(IllegalStateException e) {
            return "pierwszy";
        }

        @ExceptionHandler(IllegalStateException.class)
        String second(IllegalStateException e) {
            return "drugi";
        }
    }
}
