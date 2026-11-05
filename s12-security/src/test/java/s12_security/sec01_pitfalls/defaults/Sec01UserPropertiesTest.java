package s12_security.sec01_pitfalls.defaults;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Sekcja 1 lekcji Sec01Basics — sam starter, wariant 3 z 3: nazwa i hasło z właściwości (hasło TYLKO DO NAUKI).
 */
@SpringBootTest(properties = {"spring.security.user.name=user", "spring.security.user.password=haslo-testowe"})
@AutoConfigureMockMvc
class Sec01UserPropertiesTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 1: bez danych 401 z WWW-Authenticate: Basic, z Basic user/haslo-testowe 200")
    void basicForApi() {
        assertThat(mvc.get().uri("/sec01/defaults/hello"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .headers().containsHeader(HttpHeaders.WWW_AUTHENTICATE);
        assertThat(mvc.get().uri("/sec01/defaults/hello").with(httpBasic("user", "haslo-testowe")))
                .hasStatusOk()
                .hasBodyTextEqualTo("Witaj w sklepie");
    }

    @Test
    @DisplayName("Sekcja 1: przeglądarka bez logowania → 302 na wygenerowany formularz /login")
    void formForBrowser() {
        assertThat(mvc.get().uri("/sec01/defaults/hello").accept(MediaType.TEXT_HTML))
                .hasStatus(HttpStatus.FOUND)
                .hasHeader(HttpHeaders.LOCATION, "/login");
    }
}
