package s12_security.sec04_stateless_api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * ĆWICZENIE 4 (trudniejsze) lekcji Sec04StatelessApi — własny punkt wejścia (entry point) z odpowiedzią JSON.
 *
 * <p>Zaimplementuj {@code commence} tak, żeby odpowiedź miała: status 401, nagłówek
 * {@code WWW-Authenticate: Bearer}, typ zawartości {@code application/problem+json} i ciało JSON z polami
 * {@code status} (401), {@code title} („Unauthorized”), {@code detail} („Zaloguj się, żeby kontynuować.”)
 * i {@code instance} (ścieżka zapytania, np. /api/orders).</p>
 *
 * <p>Podpowiedź: ProblemDetailSecurityHandler w tym pakiecie (ProblemDetail + JacksonJsonHttpMessageConverter).
 * Wolno też złożyć JSON ręcznie — test czyta go jako JSON, kolejność pól nie ma znaczenia.
 * Celowo bez {@code @Component}: klasa nie jest beanem aplikacji lekcji.</p>
 */
public class ExerciseJsonEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
