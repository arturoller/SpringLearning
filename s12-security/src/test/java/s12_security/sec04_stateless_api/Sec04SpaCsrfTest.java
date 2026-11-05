package s12_security.sec04_stateless_api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Sekcja 2 lekcji Sec04StatelessApi: CSRF dla aplikacji SPA ({@code csrf.spa()} — gotowe w Security 7.1). Serwer wysyła token
 * w ciasteczku {@code XSRF-TOKEN}; skrypt z NASZEJ strony czyta je i odsyła w nagłówku {@code X-XSRF-TOKEN}. Obca strona
 * ciasteczka nie przeczyta (przeglądarka nie pozwala), więc nagłówka nie podrobi.
 */
@WebMvcTest
@ContextConfiguration(classes = {BrowserPageController.class, SpaCsrfConfig.class})
class Sec04SpaCsrfTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 2: csrf.spa() — ciasteczko XSRF-TOKEN bez HttpOnly; POST z nagłówkiem X-XSRF-TOKEN 200, bez niego 403")
    void spaCsrfToken() {
        MvcTestResult page = mvc.get().uri("/sec04/web/profile").with(httpBasic("anna", "haslo-testowe")).exchange();
        Cookie token = page.getResponse().getCookie("XSRF-TOKEN");
        assertThat(token).as("serwer wysłał token w ciasteczku").isNotNull();
        assertThat(token.isHttpOnly()).as("JavaScript musi umieć przeczytać ciasteczko").isFalse();

        assertThat(mvc.post().uri("/sec04/web/email").param("email", "anna@sklep.example")
                .with(httpBasic("anna", "haslo-testowe"))
                .cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .hasStatusOk();
        assertThat(mvc.post().uri("/sec04/web/email").param("email", "napastnik@zly.example")
                .with(httpBasic("anna", "haslo-testowe"))
                .cookie(token))                      // samo ciasteczko — tak wygląda żądanie z obcej strony
                .hasStatus(HttpStatus.FORBIDDEN);
    }
}
