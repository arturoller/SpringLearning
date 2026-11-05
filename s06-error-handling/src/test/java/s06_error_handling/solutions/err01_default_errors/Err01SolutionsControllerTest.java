package s06_error_handling.solutions.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy rozwiązań wzorcowych lekcji Err01DefaultErrors. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
@WebMvcTest(Err01SolutionsController.class)
class Err01SolutionsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Wzorzec 1: znane zamówienie → 200, nieznane → 404 z opisem")
    void exercise1() throws Exception {
        mockMvc.perform(get("/err01/cw/orders/2")).andExpect(status().isOk()).andExpect(content().string("Zamówienie #2"));
        var response = mockMvc.perform(get("/err01/cw/orders/99")).andExpect(status().isNotFound()).andReturn().getResponse();
        assertThat(response.getErrorMessage()).isEqualTo("Nie ma zamówienia 99");
    }

    @Test
    @DisplayName("Wzorzec 2: brak towaru (kawa) → 409 dzięki @ResponseStatus na wyjątku, inny towar → 200")
    void exercise2() throws Exception {
        mockMvc.perform(get("/err01/cw/stock/kawa")).andExpect(status().isConflict());
        mockMvc.perform(get("/err01/cw/stock/herbata")).andExpect(status().isOk()).andExpect(content().string("Jest: herbata"));
    }

    @Test
    @DisplayName("Wzorzec 3: rabat 0-100 → 200, poza zakresem → 400 z opisem")
    void exercise3() throws Exception {
        mockMvc.perform(get("/err01/cw/discount?percent=15")).andExpect(status().isOk()).andExpect(content().string("Rabat: 15%"));
        mockMvc.perform(get("/err01/cw/discount?percent=100")).andExpect(status().isOk());
        var response = mockMvc.perform(get("/err01/cw/discount?percent=101")).andExpect(status().isBadRequest()).andReturn().getResponse();
        assertThat(response.getErrorMessage()).isEqualTo("Rabat musi być w przedziale 0-100");
        mockMvc.perform(get("/err01/cw/discount?percent=-1")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Wzorzec 4: summarize — jedna linia z domyślnego ciała błędu, z komunikatem tylko gdy jest")
    void exercise4() {
        assertThat(Err01SolutionsController.summarize(Map.of("status", 404, "error", "Not Found", "path", "/err01/x")))
                .isEqualTo("404 Not Found (/err01/x)");
        assertThat(Err01SolutionsController.summarize(Map.of("status", 409, "error", "Conflict", "path", "/err01/stock/kawa",
                "message", "Brak towaru: kawa"))).isEqualTo("409 Conflict (/err01/stock/kawa): Brak towaru: kawa");
        assertThat(Err01SolutionsController.summarize(Map.of("status", 500, "error", "Internal Server Error", "path", "/p", "message", "")))
                .isEqualTo("500 Internal Server Error (/p)");
    }

    @Test
    @DisplayName("Wzorzec 5: leakedKeys — wykrywa trace, exception, errors i message z danymi technicznymi")
    void exercise5() {
        assertThat(Err01SolutionsController.leakedKeys(Map.of("status", 500, "error", "Internal Server Error", "path", "/p"))).isEmpty();
        assertThat(Err01SolutionsController.leakedKeys(Map.of("status", 500, "trace", "…", "exception", "x.Y", "errors", List.of())))
                .containsExactly("errors", "exception", "trace");
        assertThat(Err01SolutionsController.leakedKeys(Map.of("message", "Brak towaru: kawa"))).isEmpty();
        assertThat(Err01SolutionsController.leakedKeys(Map.of("message", "połączenie z db-prod-01 odrzucone"))).containsExactly("message");
    }
}
