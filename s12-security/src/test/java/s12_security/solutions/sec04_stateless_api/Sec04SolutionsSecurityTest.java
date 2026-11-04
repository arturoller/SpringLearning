package s12_security.solutions.sec04_stateless_api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import s12_security.sec04_stateless_api.ExerciseController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Test rozwiązania wzorcowego ćwiczenia 4 lekcji Sec04StatelessApi. @Tag("wzorzec"). Kontroler jest ten sam, gotowy
 * kontroler z lekcji; reguły — z rozwiązania ({@link ExerciseSecurityConfig}).
 */
@Tag("wzorzec")
@WebMvcTest
@ContextConfiguration(classes = {ExerciseController.class, ExerciseSecurityConfig.class})
class Sec04SolutionsSecurityTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Wzorzec 4: katalog publiczny, zamówienia bez logowania → 401 z JSON-em z ćwiczenia 3")
    void anonymous() {
        assertThat(mvc.get().uri("/sec04/cw/catalog")).hasStatusOk();
        assertThat(mvc.get().uri("/sec04/cw/orders"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.title").isEqualTo("Zaloguj się");
        assertThat(mvc.get().uri("/sec04/cw/orders").with(httpBasic("anna", "zle-haslo")))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"cw\"");
    }

    @Test
    @DisplayName("Wzorzec 4: logowanie Basic działa, ale serwer nie zakłada sesji; POST bez tokenu CSRF → 201")
    void statelessWithoutCsrf() {
        MvcTestResult result = mvc.get().uri("/sec04/cw/orders").with(httpBasic("anna", "haslo-testowe")).exchange();
        assertThat(result).hasStatusOk().hasBodyTextEqualTo("Zamówienia: anna");
        assertThat(result.getMvcResult().getRequest().getSession(false)).isNull();
        assertThat(mvc.post().uri("/sec04/cw/orders").with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("Wzorzec 4: preflight z http://localhost:3000 bez logowania → 200 z nagłówkami CORS")
    void preflight() {
        assertThat(mvc.options().uri("/sec04/cw/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                .hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800");
    }
}
