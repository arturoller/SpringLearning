package s06_error_handling.err02_exception_handler;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inny kontroler, który rzuca TEN SAM PaymentException — ale handlery z PaymentController go tu NIE widzą,
 * bo {@code @ExceptionHandler} w klasie kontrolera działa tylko dla jej własnych metod.
 */
@RestController
public class OrderController {

    @PostMapping("/api/orders/{id}/pay")
    public String pay(@PathVariable String id) {
        throw new PaymentException("Płatność za zamówienie " + id + " nie powiodła się");
    }
}
