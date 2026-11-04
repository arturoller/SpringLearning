package s05_validation.val02_valid_in_controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Kontroler z PROSTĄ obsługą błędów walidacji — tylko zapowiedź. Pełne, wspólne dla całej aplikacji rozwiązanie
 * ({@code @RestControllerAdvice}, {@code ProblemDetail}) jest w s06_error_handling/Err02ExceptionHandler,
 * s06_error_handling/Err03ControllerAdvice i s06_error_handling/Err04ProblemDetail.
 */
@RestController
@RequestMapping("/val02/friendly/products")
public class FriendlyProductController {

    private final ProductService service;

    public FriendlyProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public Product find(@PathVariable @Min(value = 1, message = "Id musi być dodatnie") long id) {
        return service.find(id).orElseThrow();
    }

    /** Błędne CIAŁO ({@code @Valid @RequestBody}): mapa pole → komunikat, posortowana po nazwie pola. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onInvalidBody(MethodArgumentNotValidException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .sorted(java.util.Comparator.comparing(FieldError::getField)
                        .thenComparing(error -> String.valueOf(error.getDefaultMessage())))
                .collect(Collectors.toMap(FieldError::getField, error -> String.valueOf(error.getDefaultMessage()),
                        (first, second) -> first + "; " + second, TreeMap::new));
    }

    /** Błędny PARAMETR ({@code @PathVariable @Min}): lista komunikatów, posortowana. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<String> onInvalidParameter(HandlerMethodValidationException exception) {
        return exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .sorted()
                .toList();
    }
}
