package s06_error_handling.solutions.err02_exception_handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import s06_error_handling.err02_exception_handler.ApiError;

import java.util.Map;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err02ExceptionHandler — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/err02/cw")
public class Err02SolutionsController {

    public static class CustomerNotFoundException extends RuntimeException {

        public CustomerNotFoundException(long id) {
            super("Nie ma klienta " + id);
        }
    }

    public static class OrderAlreadyShippedException extends RuntimeException {

        public OrderAlreadyShippedException(long id) {
            super("Zamówienie " + id + " zostało już wysłane");
        }
    }

    public static class InvoiceException extends RuntimeException {

        public InvoiceException(String message) {
            super(message);
        }
    }

    public static class InvoiceExpiredException extends InvoiceException {

        public InvoiceExpiredException() {
            super("Faktura jest przeterminowana");
        }
    }

    public static class WarehouseClosedException extends RuntimeException {
    }

    public static class WarehouseOverloadedException extends RuntimeException {
    }

    public record CustomerRequest(@NotBlank(message = "Imię jest wymagane") String name,
                                  @Email(message = "Zły adres e-mail") @NotBlank(message = "E-mail jest wymagany") String email) {
    }

    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        if (id == 1) {
            return "Anna";
        }
        throw new CustomerNotFoundException(id);
    }

    @PostMapping("/orders/{id}/ship")
    public String ship(@PathVariable long id) {
        throw new OrderAlreadyShippedException(id);
    }

    @GetMapping("/invoices/{kind}")
    public String invoice(@PathVariable String kind) {
        return switch (kind) {
            case "stare" -> throw new InvoiceExpiredException();
            case "bledna" -> throw new InvoiceException("Faktura jest błędna");
            default -> "OK";
        };
    }

    @PostMapping("/customers")
    public String create(@Valid @RequestBody CustomerRequest request) {
        return "Utworzono: " + request.name();
    }

    @GetMapping("/warehouse/{mode}")
    public String warehouse(@PathVariable String mode) {
        return switch (mode) {
            case "closed" -> throw new WarehouseClosedException();
            case "overloaded" -> throw new WarehouseOverloadedException();
            default -> "OK";
        };
    }

    // ------------------------------------------------------------------ handlery

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> onCustomerNotFound(CustomerNotFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, "CUSTOMER_NOT_FOUND", exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(OrderAlreadyShippedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError onAlreadyShipped(OrderAlreadyShippedException exception, HttpServletRequest request) {
        return new ApiError(409, "ALREADY_SHIPPED", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvoiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError onInvoice(InvoiceException exception, HttpServletRequest request) {
        return new ApiError(400, "INVOICE_ERROR", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvoiceExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiError onInvoiceExpired(InvoiceExpiredException exception, HttpServletRequest request) {
        return new ApiError(410, "INVOICE_EXPIRED", exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onInvalid(MethodArgumentNotValidException exception) {
        Map<String, String> messages = new TreeMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            messages.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (first, second) -> first + "; " + second);
        }
        return messages;
    }

    @ExceptionHandler({WarehouseClosedException.class, WarehouseOverloadedException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError onWarehouse(RuntimeException exception, HttpServletRequest request) {
        return new ApiError(503, exception.getClass().getSimpleName(), "Magazyn niedostępny", request.getRequestURI());
    }
}
