package s12_security.sec04_pitfalls.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/** Sekcja 4 lekcji Sec04StatelessApi — PUŁAPKA: {@code @RestControllerAdvice} nie obsługuje błędów z filtrów bezpieczeństwa. */
@SpringBootTest
@AutoConfigureMockMvc
class Sec04AdvicePitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 4 (pułapka): 401 i 403 z reguł adresów mają puste ciało — advice z handlerami błędów Security nie został wywołany")
    void adviceDoesNotSeeFilterErrors() {
        AdviceApplication.ADVICE_CALLS.set(0);
        assertThat(mvc.get().uri("/sec04/pitfall/admin/stats"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasBodyTextEqualTo("");
        assertThat(mvc.get().uri("/sec04/pitfall/admin/stats").with(httpBasic("anna", "haslo-testowe")))
                .hasStatus(HttpStatus.FORBIDDEN)
                .hasBodyTextEqualTo("");
        assertThat(AdviceApplication.ADVICE_CALLS).hasValue(0);
    }
}
