package s12_security.sec04_stateless_api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;

/**
 * Błędy bezpieczeństwa jako ProblemDetail (RFC 9457, s06_error_handling/Err04ProblemDetail) — sekcja 4 lekcji Sec04StatelessApi.
 * <ul>
 *   <li>{@link AuthenticationEntryPoint} (punkt wejścia uwierzytelniania) — "nie wiem, kim jesteś" → 401.</li>
 *   <li>{@link AccessDeniedHandler} (obsługa odmowy dostępu) — "wiem, kim jesteś, ale nie wolno ci" → 403.</li>
 * </ul>
 * Obie metody wołają FILTRY Spring Security, a nie DispatcherServlet — dlatego {@code @RestControllerAdvice} ich nie zastąpi
 * (PUŁAPKA w lekcji). Odpowiedź piszemy sami: status, nagłówki i JSON (JsonMapper — Jackson 3, bean z Boota).
 */
public class ProblemDetailSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public ProblemDetailSecurityHandlers(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        // 401 MUSI mieć nagłówek WWW-Authenticate ("jak się zalogować") — tak mówi standard HTTP (RFC 9110).
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"sklep\"");
        write(response, request, HttpStatus.UNAUTHORIZED, "Wymagane logowanie",
                "Podaj poprawne dane logowania w nagłówku Authorization.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "Brak uprawnień",
                "Twoje konto nie ma dostępu do tego zasobu.");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String title,
                       String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        // Celowo bez exception.getMessage(): komunikaty techniczne ("Bad credentials", nazwy klas) zostają w logach.
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
