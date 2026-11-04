package s06_error_handling.solutions.err02_exception_handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy rozwiązań wzorcowych lekcji Err02ExceptionHandler. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
@WebMvcTest(Err02SolutionsController.class)
class Err02SolutionsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: znany klient → 200, nieznany → 404 z ciałem ApiError")
    void exercise1() throws Exception {
        mockMvc.perform(get("/err02/cw/customers/1")).andExpect(status().isOk()).andExpect(content().string("Anna"));
        mockMvc.perform(get("/err02/cw/customers/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nie ma klienta 7"))
                .andExpect(jsonPath("$.path").value("/err02/cw/customers/7"));
    }

    @Test
    @DisplayName("Wzorzec 2: wysłane zamówienie → 409 i kod ALREADY_SHIPPED")
    void exercise2() throws Exception {
        mockMvc.perform(post("/err02/cw/orders/5/ship"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SHIPPED"))
                .andExpect(jsonPath("$.message").value("Zamówienie 5 zostało już wysłane"))
                .andExpect(jsonPath("$.path").value("/err02/cw/orders/5/ship"));
    }

    @Test
    @DisplayName("Wzorzec 3: faktura błędna → 400, przeterminowana → 410 (bliższy typ wygrywa), dobra → 200")
    void exercise3() throws Exception {
        mockMvc.perform(get("/err02/cw/invoices/bledna")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVOICE_ERROR")).andExpect(jsonPath("$.message").value("Faktura jest błędna"));
        mockMvc.perform(get("/err02/cw/invoices/stare")).andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVOICE_EXPIRED"));
        mockMvc.perform(get("/err02/cw/invoices/nowa")).andExpect(status().isOk()).andExpect(content().string("OK"));
    }

    @Test
    @DisplayName("Wzorzec 4: błędne dane klienta → 400 i posortowana mapa pole → komunikat")
    void exercise4() throws Exception {
        mockMvc.perform(post("/err02/cw/customers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \",\"email\":\"zly\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Zły adres e-mail"))
                .andExpect(jsonPath("$.name").value("Imię jest wymagane"));
        mockMvc.perform(post("/err02/cw/customers").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ewa\",\"email\":\"ewa@example.com\"}"))
                .andExpect(status().isOk()).andExpect(content().string("Utworzono: Ewa"));
    }

    @Test
    @DisplayName("Wzorzec 5: jeden handler dla dwóch wyjątków magazynu → 503 z kodem z nazwy klasy")
    void exercise5() throws Exception {
        mockMvc.perform(get("/err02/cw/warehouse/closed")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("WarehouseClosedException")).andExpect(jsonPath("$.message").value("Magazyn niedostępny"));
        mockMvc.perform(get("/err02/cw/warehouse/overloaded")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("WarehouseOverloadedException"));
        mockMvc.perform(get("/err02/cw/warehouse/open")).andExpect(status().isOk());
    }
}
