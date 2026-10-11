package s06_error_handling.err01_default_errors;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 2 lekcji Err01DefaultErrors: co widzi MockMvc. Wybieramy {@code @WebMvcTest} (tylko warstwa web), bo
 * sprawdzamy zachowanie samego DispatcherServlet, a nie całej aplikacji. MockMvc NIE ma prawdziwego kontenera
 * serwletów (Tomcata), więc nie przekierowuje błędu na /error.
 */
@WebMvcTest(Err01Controller.class)
class Err01DefaultErrorsMockMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("2. MockMvc: niezłapany wyjątek NIE staje się odpowiedzią 500 — wyjątek wylatuje z perform()")
    void uncaughtExceptionPropagates() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/boom")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Brak połączenia z bazą jdbc:postgresql://db.intranet:5432/shop (użytkownik shop_user)");
    }

    @Test
    @DisplayName("2. MockMvc: błąd 400 ma status, ale puste ciało i brak przekierowania na /error")
    void statusWithoutBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();       // brak JSON-a z timestamp/status/error/path
        assertThat(result.getResponse().getForwardedUrl()).isNull();           // nikt nie przekazał żądania do /error
    }

    @Test
    @DisplayName("2. MockMvc: nieistniejący adres daje 404 z pustym ciałem")
    void notFoundWithoutBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/nie-ma")).andExpect(status().isNotFound()).andReturn();

        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }
}
