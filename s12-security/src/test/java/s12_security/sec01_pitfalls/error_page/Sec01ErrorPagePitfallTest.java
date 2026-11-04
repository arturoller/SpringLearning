package s12_security.sec01_pitfalls.error_page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Sec01Basics — PUŁAPKA {@code /error}. Prawdziwy serwer (RANDOM_PORT), bo przekazanie na {@code /error}
 * robi Tomcat, a MockMvc go nie ma. Porównaj z Sec01ServerTest (poprawna konfiguracja: 500 i 404).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.security.user.name=anna", "spring.security.user.password=haslo-testowe"})
class Sec01ErrorPagePitfallTest {

    @LocalServerPort
    int port;

    private int status(String uri) {
        return RestClient.create("http://localhost:" + port).get().uri(uri)
                .exchange((request, response) -> response.getStatusCode().value());
    }

    private int statusAsAnna(String uri) {
        return RestClient.create("http://localhost:" + port).get().uri(uri)
                .headers(headers -> headers.setBasicAuth("anna", "haslo-testowe"))
                .exchange((request, response) -> response.getStatusCode().value());
    }

    @Test
    @DisplayName("Sekcja 5 (pułapka): bez permitAll() dla /error awaria publicznego adresu wygląda jak 401, a brak strony też jak 401")
    void errorsBecome401() {
        assertThat(status("/sec01/pitfall/public/crash")).isEqualTo(401);   // naprawdę: 500
        assertThat(status("/sec01/pitfall/public/nie-ma")).isEqualTo(401);  // naprawdę: 404
    }

    @Test
    @DisplayName("Sekcja 5 (pułapka): zalogowany widzi prawdziwe 500 i 404 — dlatego błąd łatwo przeoczyć, testując jako zalogowany")
    void loggedInUserSeesRealErrors() {
        assertThat(statusAsAnna("/sec01/pitfall/public/crash")).isEqualTo(500);
        assertThat(statusAsAnna("/sec01/pitfall/public/nie-ma")).isEqualTo(404);
    }
}
