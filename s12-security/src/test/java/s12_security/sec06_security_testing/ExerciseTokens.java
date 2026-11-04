package s12_security.sec06_security_testing;

import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * SZKIELET ĆWICZENIA 3 lekcji Sec06SecurityTesting: prawdziwy token w teście MockMvc — bez atrapy {@code jwt()}.
 */
public final class ExerciseTokens {

    private ExerciseTokens() {
    }

    /**
     * Ćwiczenie 3: wyślij {@code POST /sec06/token} z HTTP Basic (hasło {@code haslo-testowe}), wyjmij {@code access_token}
     * z odpowiedzi JSON i zwróć gotową wartość nagłówka: {@code "Bearer " + token}.
     */
    public static String bearerFor(MockMvcTester mvc, String username) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
