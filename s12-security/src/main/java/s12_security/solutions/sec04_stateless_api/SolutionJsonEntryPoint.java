package s12_security.solutions.sec04_stateless_api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.net.URI;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec04StatelessApi (ExerciseJsonEntryPoint). */
public class SolutionJsonEntryPoint implements AuthenticationEntryPoint {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Zaloguj się, żeby kontynuować.");
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
    }
}
