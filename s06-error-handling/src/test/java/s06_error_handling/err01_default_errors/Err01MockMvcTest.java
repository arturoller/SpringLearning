package s06_error_handling.err01_default_errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * PUŁAPKA: MockMvc woła DispatcherServlet bez prawdziwego kontenera servletów, więc NIE ma przekierowania na /error
 * (to robi dopiero Tomcat). Dlatego test ciała błędu Boota musi iść przez RANDOM_PORT (Err01DefaultErrorsTest).
 */
@SpringBootTest
@AutoConfigureMockMvc   // auto configure = skonfiguruj automatycznie; mock MVC = atrapa serwera MVC (bez sieci)
class Err01MockMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. MockMvc: 404 jest, ale ciała błędu (JSON z /error) NIE ma — puste ciało")
    void mockMvcDoesNotRenderErrorBody() throws Exception {
        var result = mockMvc.perform(get("/api/nie-ma-takiego")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("1b. MockMvc: sprawdzanie pola JSON na odpowiedzi 404 kończy się porażką asercji (ZNAJDŹ BŁĄD z pytań)")
    void jsonPathOnMockMvcErrorFails() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/nie-ma-takiego"))
                .andExpect(jsonPath("$.status").value(404)))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @DisplayName("2. MockMvc: nieobsłużony wyjątek nie zamienia się w 500 z JSON-em — leci prosto do testu")
    void mockMvcPropagatesUnhandledException() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/products/KSI-999")))
                .isInstanceOf(jakarta.servlet.ServletException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Brak produktu KSI-999");
    }
}
