package s06_error_handling.solutions.err05_custom_exceptions;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy rozwiązań wzorcowych lekcji Err05CustomExceptions. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
@WebMvcTest(Err05SolutionsController.class)
class Err05SolutionsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: znany klient → 200, nieznany → 404 ProblemDetail z kodem i adresem type")
    void exercise1() throws Exception {
        mockMvc.perform(get("/err05/cw/customers/1")).andExpect(status().isOk()).andExpect(content().string("Anna"));
        mockMvc.perform(get("/err05/cw/customers/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Nie znaleziono zasobu"))
                .andExpect(jsonPath("$.detail").value("Nie ma klienta 9"))
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/customer-not-found"));
    }

    @Test
    @DisplayName("Wzorzec 2: odrzucona płatność (reguła biznesowa) → 422 i kod PAYMENT_DECLINED")
    void exercise2() throws Exception {
        mockMvc.perform(post("/err05/cw/payments/5"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Naruszona reguła biznesowa"))
                .andExpect(jsonPath("$.code").value("PAYMENT_DECLINED"))
                .andExpect(jsonPath("$.type").value("https://sklep.example/errors/payment-declined"));
    }

    @Test
    @DisplayName("Wzorzec 3: faktura już opłacona → 409 (najbliższy typ), a płatność odrzucona nadal 422")
    void exercise3() throws Exception {
        mockMvc.perform(post("/err05/cw/invoices/3/pay"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Konflikt ze stanem sklepu"))
                .andExpect(jsonPath("$.code").value("INVOICE_ALREADY_PAID"));
        mockMvc.perform(post("/err05/cw/payments/5")).andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("Wzorzec 4: nieprzewidziany wyjątek → 500 bez tajnych szczegółów, a zła metoda HTTP nadal 405")
    void exercise4() throws Exception {
        mockMvc.perform(get("/err05/cw/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNEXPECTED"))
                .andExpect(jsonPath("$.detail").value("Wystąpił nieoczekiwany błąd"))
                .andExpect(content().string(not(containsString("hasło"))));
        mockMvc.perform(delete("/err05/cw/crash")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Wzorzec 5: magazyn zajęty → 503, nagłówek Retry-After i właściwość retryAfterSeconds")
    void exercise5() throws Exception {
        mockMvc.perform(post("/err05/cw/stock/refresh"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("WAREHOUSE_BUSY"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(30));
    }
}
