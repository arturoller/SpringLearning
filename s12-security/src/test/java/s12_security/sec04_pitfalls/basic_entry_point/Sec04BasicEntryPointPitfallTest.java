package s12_security.sec04_pitfalls.basic_entry_point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/** Sekcja 4 lekcji Sec04StatelessApi — PUŁAPKA: punkt wejścia tylko w exceptionHandling, a nie w httpBasic. */
@SpringBootTest
@AutoConfigureMockMvc
class Sec04BasicEntryPointPitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 4 (pułapka): brak logowania → nasz JSON, ale złe hasło → domyślne 401 z pustym ciałem i realm=\"Realm\"")
    void wrongPasswordSkipsCustomEntryPoint() {
        assertThat(mvc.get().uri("/sec04/pitfall/orders"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.title").isEqualTo("Wymagane logowanie");
        assertThat(mvc.get().uri("/sec04/pitfall/orders").with(httpBasic("anna", "zle-haslo")))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm\", charset=\"UTF-8\"")
                .hasBodyTextEqualTo("");
    }
}
