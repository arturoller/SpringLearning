package s12_security.solutions.sec06_security_testing;

import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Sec06SecurityTesting. */
public final class ExerciseTokens {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ExerciseTokens() {
    }

    public static String bearerFor(MockMvcTester mvc, String username) {
        MvcTestResult result = mvc.post().uri("/sec06/token").with(httpBasic(username, "haslo-testowe")).exchange();
        assertThat(result).hasStatusOk();          // czytelny błąd, gdy logowanie się nie uda
        String token = JSON.readTree(result.getResponse().getContentAsByteArray()).get("access_token").asString();
        return "Bearer " + token;
    }
}
