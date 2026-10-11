package s06_error_handling.err04_problem_detail;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

/** Kontroler z różnymi źródłami błędów: wyjątek domenowy, wyjątki Springa MVC, ResponseStatusException, ErrorResponseException. */
@RestController
public class ProductController {

    @GetMapping("/api/products/{id}")
    public Map<String, Object> product(@PathVariable long id) {
        if (id == 999) {
            throw new ProductNotFoundException(id);
        }
        return Map.of("id", id, "name", "Kawa");
    }

    /** Parametr q jest WYMAGANY: brak → MissingServletRequestParameterException (wyjątek Springa MVC, 400). */
    @GetMapping("/api/search")
    public Map<String, Object> search(@RequestParam String q) {
        return Map.of("query", q);
    }

    @GetMapping("/api/products/{id}/stock")
    public Map<String, Object> stock(@PathVariable long id) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Towar chwilowo niedostępny: " + id);
    }

    /**
     * ErrorResponseException = wyjątek, który SAM niesie gotowy ProblemDetail (status, tytuł, typ, własne pola).
     * Przydatny, gdy chcesz rzucić "kompletną" odpowiedź błędu z dowolnego miejsca kodu.
     */
    @GetMapping("/api/limits")
    public String limits() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Limit zapytań przekroczony");
        problem.setTitle("Zbyt wiele zapytań");
        problem.setType(URI.create("https://shop.example.com/problems/rate-limit"));
        problem.setProperty("retryAfterSeconds", 30);
        throw new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, problem, null);
    }
}
