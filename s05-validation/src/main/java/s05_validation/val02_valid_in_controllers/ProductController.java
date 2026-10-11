package s05_validation.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * ProductController = kontroler produktów z walidacją i z własną obsługą błędów (sekcje 1-4 lekcji Val02ValidInControllers).
 * Klasa NIE ma {@code @Validated} — działa wbudowana walidacja parametrów z Frameworka 6.1+ (sekcja 3).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** ProductRequest = dane produktu z formularza (treść żądania JSON). Reguły to adnotacje przy komponentach. */
    public record ProductRequest(
            @NotBlank(message = "Nazwa jest wymagana") @Size(max = 40, message = "Nazwa: najwyżej 40 znaków") String name,
            @Pattern(regexp = "[A-Z]{3}-\\d{4}", message = "Kod ma postać ABC-1234") String code,
            @Positive(message = "Cena musi być dodatnia") int priceGrosze) {
    }

    /** ProductResponse = odpowiedź po zapisaniu produktu (identyfikator nadaje „serwer”). */
    public record ProductResponse(long id, String name, String code, int priceGrosze) {
    }

    /** ErrorBody = czytelny JSON błędu: lista par (pole, komunikat). */
    public record ErrorBody(String error, List<FieldProblem> problems) {
    }

    /** FieldProblem = jeden problem: nazwa pola lub parametru i komunikat. */
    public record FieldProblem(String field, String message) {
    }

    // --- sekcja 1: @Valid @RequestBody ---------------------------------------------------------------

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ProductResponse(1, request.name(), request.code(), request.priceGrosze()));
    }

    // --- sekcja 3: ograniczenia na @RequestParam i @PathVariable -------------------------------------

    @GetMapping
    public Map<String, Object> list(@RequestParam @Positive @Max(100) int limit,
                                    @RequestParam(required = false) @Size(min = 2, max = 20) String search) {
        return Map.of("limit", limit, "search", search == null ? "" : search);
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable @Min(1) long id) {
        return Map.of("id", id);
    }

    // --- sekcja 4: ograniczenie na zmiennej ścieżki RAZEM z @Valid @RequestBody ----------------------

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable @Min(1) long id, @Valid @RequestBody ProductRequest request) {
        return new ProductResponse(id, request.name(), request.code(), request.priceGrosze());
    }

    // --- sekcja 2: zamiana wyjątków na czysty JSON ---------------------------------------------------

    /** Błędy pól z {@code @Valid @RequestBody}: MethodArgumentNotValidException (nieprawidłowy argument metody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> onBodyInvalid(MethodArgumentNotValidException e) {
        List<FieldProblem> problems = e.getBindingResult().getFieldErrors().stream()
                .map((FieldError fe) -> new FieldProblem(fe.getField(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldProblem::field))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorBody("Niepoprawne dane", problems));
    }

    /**
     * Błędy parametrów: HandlerMethodValidationException (Framework 6.1+). Trafia tu także ciało {@code @Valid @RequestBody},
     * gdy ta sama metoda ma jeszcze ograniczenia na parametrach (sekcja 4) — wtedy wynik dla ciała to
     * {@code ParameterErrors} (błędy pól obiektu), a dla zwykłego parametru — lista komunikatów.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorBody> onParametersInvalid(HandlerMethodValidationException e) {
        List<FieldProblem> problems = new ArrayList<>();
        e.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors bodyErrors) {          // ciało: znamy nazwy pól
                bodyErrors.getFieldErrors().forEach(fe -> problems.add(new FieldProblem(fe.getField(), fe.getDefaultMessage())));
            } else {                                                      // parametr: nazwa parametru metody
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error -> problems.add(new FieldProblem(name, error.getDefaultMessage())));
            }
        });
        problems.sort(Comparator.comparing(FieldProblem::field).thenComparing(FieldProblem::message));
        return ResponseEntity.badRequest().body(new ErrorBody("Niepoprawne parametry", problems));
    }
}
