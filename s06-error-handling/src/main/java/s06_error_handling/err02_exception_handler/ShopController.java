package s06_error_handling.err02_exception_handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Map;
import java.util.TreeMap;

/**
 * Kontroler lekcji Err02ExceptionHandler. Punkty końcowe rzucają wyjątki, a metody oznaczone {@code @ExceptionHandler}
 * (handler = procedura obsługi) zamieniają je na odpowiedzi. Handlery działają TYLKO dla tego kontrolera.
 */
@RestController
@RequestMapping("/err02/shop")
public class ShopController {

    private static final Logger log = LoggerFactory.getLogger(ShopController.class);

    // ------------------------------------------------------------------ punkty końcowe

    @GetMapping("/products/{id}")
    public Product product(@PathVariable long id) {
        return switch ((int) id) {
            case 1 -> new Product(1, "Kawa");
            case 2 -> new Product(2, "Herbata");
            case 3 -> throw new BrokenDataException();
            default -> throw new ProductNotFoundException(id);
        };
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody NewProduct request) {
        return new Product(3, request.name());
    }

    /** Ograniczenie {@code @Min} na parametrze zmienia typ wyjątku walidacji (Val02ValidInControllers, sekcja 4). */
    @PutMapping("/products/{id}")
    public Product replace(@PathVariable @Min(value = 1, message = "Id musi być dodatnie") long id, @Valid @RequestBody NewProduct request) {
        return new Product(id, request.name());
    }

    @PostMapping("/products/{id}/reserve")
    public String reserve(@PathVariable long id) {
        if (id == 1) {
            throw new OutOfStockException("Kawa");
        }
        if (id == 2) {
            throw new WarehouseClosedException();
        }
        return "Zarezerwowano";
    }

    @PostMapping("/pay")
    public String pay(@RequestParam String card) {
        return switch (card) {
            case "0000" -> throw new CardDeclinedException();
            case "1111" -> throw new PaymentException("Płatność wygasła");
            default -> "Zapłacono";
        };
    }

    @GetMapping("/coupon")
    public String coupon() {
        throw new CouponExpiredException();
    }

    @GetMapping("/crash")
    public String crash() {
        throw new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'");
    }

    // ------------------------------------------------------------------ sekcja 1 i 2: handler z parametrami i typami zwracanymi

    /** Zwraca {@code ResponseEntity} — status wybierasz w kodzie, razem z ciałem. Drugi parametr: żądanie serwletowe. */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> onProductNotFound(ProductNotFoundException exception, HttpServletRequest request) {
        ApiError body = new ApiError(404, "PRODUCT_NOT_FOUND", exception.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** Zwraca sam rekord, a status ustawia {@code @ResponseStatus}. Parametr {@code WebRequest} — żądanie niezależne od serwletów. */
    @ExceptionHandler(CardDeclinedException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ApiError onCardDeclined(CardDeclinedException exception, WebRequest request) {
        return new ApiError(402, "CARD_DECLINED", exception.getMessage(), request.getDescription(false).replace("uri=", ""));   // getDescription(false) daje "uri=/adres"
    }

    /**
     * BŁĄD DLA CELÓW NAUKI: brak {@code @ResponseStatus} i brak {@code ResponseEntity}. Metoda handlera działa jak zwykła
     * metoda kontrolera — domyślny status odpowiedzi to 200 OK, więc klient dostaje 200 z opisem błędu w ciele!
     */
    @ExceptionHandler(CouponExpiredException.class)
    public ApiError onCouponExpiredForgotStatus(CouponExpiredException exception, HttpServletRequest request) {
        return new ApiError(410, "COUPON_EXPIRED", exception.getMessage(), request.getRequestURI());
    }

    // ------------------------------------------------------------------ sekcja 3: dopasowanie typów

    /** Kilka typów w jednej adnotacji — parametr musi być wspólnym nadtypem (tutaj {@code RuntimeException}). */
    @ExceptionHandler({OutOfStockException.class, WarehouseClosedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError onWarehouseProblem(RuntimeException exception, HttpServletRequest request) {
        String code = exception instanceof OutOfStockException ? "OUT_OF_STOCK" : "WAREHOUSE_CLOSED";
        return new ApiError(409, code, exception.getMessage(), request.getRequestURI());
    }

    /** Handler dla klasy bazowej {@code PaymentException}: łapie ją i wszystkie podklasy, które nie mają własnego handlera. */
    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onPayment(PaymentException exception, HttpServletRequest request) {
        return new ApiError(400, "PAYMENT_ERROR", exception.getMessage(), request.getRequestURI());
    }

    /**
     * Handler "ostatniej szansy" dla tego kontrolera. Łapie wszystko, co dziedziczy po {@code RuntimeException}, a nie ma
     * bliższego handlera. Klient dostaje ogólny komunikat; szczegóły trafiają do logu serwera.
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError onUnexpected(RuntimeException exception, HttpServletRequest request) {
        log.error("Nieoczekiwany błąd w {}", request.getRequestURI(), exception);
        return new ApiError(500, "UNEXPECTED", "Wystąpił nieoczekiwany błąd", request.getRequestURI());
    }

    // ------------------------------------------------------------------ sekcja 4: walidacja

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onInvalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> messages = new TreeMap<>();   // TreeMap: klucze posortowane, odpowiedź zawsze w tej samej kolejności
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            messages.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (first, second) -> first + "; " + second);
        }
        return messages;
    }

    /**
     * Gdy metoda ma ograniczenie na parametrze (jak {@code replace}), błędy — także błędy ciała — przychodzą jako
     * {@code HandlerMethodValidationException}, więc potrzebny jest drugi handler.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onInvalidMethod(HandlerMethodValidationException exception) {
        Map<String, String> messages = new TreeMap<>();
        exception.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                String key = error instanceof FieldError field ? field.getField() : parameter;
                messages.merge(key, String.valueOf(error.getDefaultMessage()), (first, second) -> first + "; " + second);
            });
        });
        return messages;
    }

    // ------------------------------------------------------------------ sekcja 5: handler, który sam rzuca wyjątek

    @ExceptionHandler(BrokenDataException.class)
    public ApiError onBrokenData(BrokenDataException exception) {
        throw new IllegalStateException("błąd w samym handlerze");
    }
}
