package s06_error_handling.solutions.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Err03ControllerAdvice. @Tag("wzorzec") — uruchamiasz sam (▶ obok klasy).
 * Bez Springa: {@code standaloneSetup(kontrolery).setControllerAdvice(advice)} rejestruje advice ręcznie.
 */
@Tag("wzorzec")
class Err03ControllerAdviceSolutionsTest {

    @Test
    @DisplayName("Ćwiczenie 1: kolejność advice według @Order")
    void exercise1() {
        assertThat(Err03ControllerAdviceSolutions.solution1(Map.of("Global", 3, "Payment", 2, "Order", 1, "Audit", 2)))
                .containsExactly("Order", "Audit", "Payment", "Global");
    }

    @Test
    @DisplayName("Ćwiczenie 2: handler w advice zamienia PaymentDeclinedException na 402 z JSON-em")
    void exercise2() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new Err03ControllerAdviceSolutions.Solution2Controller())
                .setControllerAdvice(new Err03ControllerAdviceSolutions.Solution2Advice())
                .build();

        mvc.perform(get("/api/ex/pay"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("Płatność odrzucona"));
    }

    @Test
    @DisplayName("Ćwiczenie 3: advice z zakresem (assignableTypes) i @Order(10) obsługuje tylko Exercise3Controller")
    void exercise3() throws Exception {
        assertThat(Err03ControllerAdviceSolutions.Solution3Advice.class.getAnnotation(RestControllerAdvice.class)).isNotNull();
        assertThat(Err03ControllerAdviceSolutions.Solution3Advice.class.getAnnotation(RestControllerAdvice.class).assignableTypes())
                .containsExactly(Err03ControllerAdviceSolutions.Solution3Controller.class);
        assertThat(Err03ControllerAdviceSolutions.Solution3Advice.class.getAnnotation(Order.class).value()).isEqualTo(10);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new Err03ControllerAdviceSolutions.Solution3Controller(), new Err03ControllerAdviceSolutions.Solution3OtherController())
                .setControllerAdvice(new Err03ControllerAdviceSolutions.Solution3Advice())
                .build();

        mvc.perform(get("/api/ex/in"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Stan niedozwolony"));
        assertThatThrownBy(() -> mvc.perform(get("/api/ex/out")))   // poza zakresem advice: wyjątek nieobsłużony
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
