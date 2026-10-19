package s12_security.sec04_stateless_api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.net.URI;

/**
 * ProblemDetailSecurityHandler = odpowiedzi 401 i 403 jako JSON w formacie ProblemDetail (RFC 9457 — „szczegóły
 * problemu”, ten sam format co w s06_error_handling/Err04ProblemDetail), zamiast pustego ciała albo strony /error.
 *
 * <ul>
 *   <li>{@code AuthenticationEntryPoint} (punkt wejścia uwierzytelniania) — co zrobić z NIEZALOGOWANYM
 *       (albo ze złym hasłem): 401.</li>
 *   <li>{@code AccessDeniedHandler} (obsługa odmowy dostępu) — co zrobić z ZALOGOWANYM bez uprawnień: 403.</li>
 * </ul>
 *
 * <p>Filtry Security działają PRZED Spring MVC, więc {@code @RestControllerAdvice} tych odmów nie złapie — odpowiedź
 * trzeba napisać tutaj. Piszemy ją tym samym konwerterem JSON, którego używa Spring MVC
 * ({@code JacksonJsonHttpMessageConverter}, Jackson 3) — dlatego JSON wygląda tak samo jak ProblemDetail
 * z kontrolera.</p>
 */
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    /** 401 — niezalogowany albo złe dane logowania. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        // RFC 9110: odpowiedź 401 MUSI mówić, jak się zalogować
        response.setHeader("WWW-Authenticate", "Basic realm=\"sklep-api\"");
        write(request, response, HttpStatus.UNAUTHORIZED, "Brak lub złe dane logowania.");
    }

    /** 403 — zalogowany, ale bez uprawnień. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "Nie masz uprawnień do tego zasobu.");
    }

    /**
     * Celowo NIE kopiujemy komunikatu wyjątku do odpowiedzi: nie zdradzamy klientowi szczegółów (np. czy login
     * istnieje) — tylko status i ogólny opis.
     */
    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(request.getRequestURI()));   // instance = którego adresu dotyczy problem
        response.setStatus(status.value());
        converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
    }
}
