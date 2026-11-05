package s06_error_handling.solutions.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy rozwiązań wzorcowych lekcji Err04ProblemDetail. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
@WebMvcTest(Err04SolutionsController.class)
class Err04SolutionsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: znany klient → 200, nieznany → 404 ProblemDetail z tytułem i kodem")
    void exercise1() throws Exception {
        mockMvc.perform(get("/err04/cw/customers/1")).andExpect(status().isOk()).andExpect(content().string("Anna"));
        mockMvc.perform(get("/err04/cw/customers/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Klient nie istnieje"))
                .andExpect(jsonPath("$.detail").value("Nie ma klienta 9"))
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/err04/cw/customers/9"));
    }

    @Test
    @DisplayName("Wzorzec 2: ErrorResponseException rzucony w kontrolerze → 409 ProblemDetail z type i kodem ORDER_LOCKED")
    void exercise2() throws Exception {
        mockMvc.perform(post("/err04/cw/orders/5/cancel"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/order-locked"))
                .andExpect(jsonPath("$.title").value("Zamówienie zablokowane"))
                .andExpect(jsonPath("$.detail").value("Zamówienie 5 jest zablokowane"))
                .andExpect(jsonPath("$.code").value("ORDER_LOCKED"));
    }

    @Test
    @DisplayName("Wzorzec 3: brak parametru q → 400 z kodem MISSING_PARAMETER i nazwą parametru")
    void exercise3() throws Exception {
        mockMvc.perform(get("/err04/cw/search").param("q", "kawa")).andExpect(status().isOk()).andExpect(content().string("Szukam: kawa"));
        mockMvc.perform(get("/err04/cw/search"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.parameter").value("q"));
    }

    @Test
    @DisplayName("Wzorzec 4: nieprzewidziany wyjątek → 500, ogólny opis, bez tajnych szczegółów")
    void exercise4() throws Exception {
        mockMvc.perform(get("/err04/cw/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNEXPECTED"))
                .andExpect(jsonPath("$.detail").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(content().string(not(containsString("hasło"))));
        mockMvc.perform(delete("/err04/cw/crash")).andExpect(status().isMethodNotAllowed());   // wyjątki Springa nadal ze swoim statusem
    }

    @Test
    @DisplayName("Wzorzec 5: błędy walidacji → 400 z mapą fields (pole → komunikat)")
    void exercise5() throws Exception {
        mockMvc.perform(post("/err04/cw/customers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ola\",\"age\":30}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Utworzono: Ola"));
        mockMvc.perform(post("/err04/cw/customers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"age\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Błąd walidacji"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.name").value("Imię jest wymagane"))
                .andExpect(jsonPath("$.fields.age").value("Klient musi być pełnoletni"));
    }
}
