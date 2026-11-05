package s12_security.sec01_basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Sec01Basics: strona błędów {@code /error} na prawdziwym serwerze. MockMvc nie przechodzi przez {@code /error}
 * (s06_error_handling/Err01DefaultErrors), więc ten test uruchamia Tomcata na losowym porcie.
 * Porównaj z pułapką: Sec01ErrorPagePitfallTest (aplikacja bez {@code permitAll()} dla {@code /error}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Sec01ServerTest {

    @LocalServerPort
    int port;

    private record Reply(HttpStatusCode status, String body) {
    }

    private Reply get(String uri) {
        return RestClient.create("http://localhost:" + port).get().uri(uri)
                .exchange((request, response) -> new Reply(response.getStatusCode(),
                        new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Sekcja 5: z permitAll() dla /error błąd publicznego adresu to prawdziwe 500 i 404, a nie 401")
    void errorPageIsPublic() {
        Reply crash = get("/sec01/public/crash");
        assertThat(crash.status().value()).isEqualTo(500);
        assertThat(crash.body()).contains("\"status\":500").doesNotContain("Awaria magazynu");   // komunikat techniczny nie wycieka
        assertThat(get("/sec01/public/nie-ma").status().value()).isEqualTo(404);
        assertThat(get("/sec01/hello").status().value()).isEqualTo(401);                          // chronione adresy dalej chronione
    }
}
