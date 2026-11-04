package s12_security.sec04_stateless_api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * SZKIELET ĆWICZENIA 3 lekcji Sec04StatelessApi: własny punkt wejścia uwierzytelniania, który odpowiada JSON-em.
 */
public class ExerciseJsonEntryPoint implements AuthenticationEntryPoint {

    /**
     * Ćwiczenie 3: status 401, nagłówek {@code WWW-Authenticate: Basic realm="cw"}, typ treści
     * {@code application/problem+json}, kodowanie UTF-8 i ciało JSON z polami {@code "status": 401} oraz
     * {@code "title": "Zaloguj się"}.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
