package s06_error_handling.err03_controller_advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Err03ControllerAdvice. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). Kontekst budujemy ręcznie
 * ({@code @ContextConfiguration}): tylko kontrolery i advice ćwiczeń, bez advice z lekcji (inaczej wyprzedziłyby Twoje handlery).
 */
@Tag("cwiczenie")
@WebMvcTest
@ContextConfiguration(classes = {ExerciseController.class, ExerciseVipController.class, ExerciseAdvice.class,
        ExerciseUrgentAdvice.class, ExerciseAdvisoryAdvice.class})
class Err03ControllerAdviceExercisesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Ćwiczenie 1: znany klient → 200, nieznany → 404 z ApiError z advice")
    void exercise1() throws Exception {
        mockMvc.perform(get("/err03/cw/customers/1")).andExpect(status().isOk()).andExpect(content().string("Anna"));
        mockMvc.perform(get("/err03/cw/customers/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nie ma klienta 9"))
                .andExpect(jsonPath("$.path").value("/err03/cw/customers/9"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: zablokowane zamówienie → 409 i kod ORDER_LOCKED")
    void exercise2() throws Exception {
        mockMvc.perform(post("/err03/cw/orders/5/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_LOCKED"))
                .andExpect(jsonPath("$.message").value("Zamówienie 5 jest zablokowane"));
    }

    @Test
    @DisplayName("Ćwiczenie 3: lokalny handler w kontrolerze VIP wygrywa z advice, a inny kontroler dalej korzysta z advice")
    void exercise3() throws Exception {
        mockMvc.perform(get("/err03/cw/vip/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VIP_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nie ma klienta VIP 9"));
        mockMvc.perform(get("/err03/cw/customers/9")).andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Ćwiczenie 4: nieprzewidziany wyjątek → 500, ogólny komunikat, bez tajnych szczegółów")
    void exercise4() throws Exception {
        mockMvc.perform(get("/err03/cw/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("UNEXPECTED"))
                .andExpect(jsonPath("$.message").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(content().string(not(containsString("hasło"))));
    }

    @Test
    @DisplayName("Ćwiczenie 5: dwa advice dla tego samego wyjątku — wygrywa ten z @Order(1) (URGENT)")
    void exercise5() throws Exception {
        mockMvc.perform(get("/err03/cw/priority"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("URGENT"));
    }
}
