package s06_error_handling.err05_custom_exceptions;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Kontroler lekcji Err05CustomExceptions: cienka warstwa nad serwisem. NIE ma w nim ani jednego {@code try/catch} ani
 * {@code @ExceptionHandler} — wyjątki serwisu lecą w górę do {@code Err05Advice}.
 */
@RestController
@RequestMapping("/err05")
public class ShopController {

    private final OrderService service;

    public ShopController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable long id) {
        return service.getProduct(id);
    }

    /** "Czy istnieje?" to zwykłe pytanie, nie błąd: odpowiedź to 200 z {@code true} albo {@code false} (Optional, nie wyjątek). */
    @GetMapping("/products/{id}/exists")
    public Map<String, Boolean> exists(@PathVariable long id) {
        return Map.of("exists", service.findProduct(id).isPresent());
    }

    @GetMapping("/orders/{id}")
    public Order order(@PathVariable long id) {
        return service.getOrder(id);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order placeOrder(@Valid @RequestBody NewOrder request) {
        return service.placeOrder(request.productId(), request.quantity());
    }

    @PostMapping("/orders/{id}/ship")
    public Order ship(@PathVariable long id) {
        return service.ship(id);
    }

    @GetMapping("/suppliers/{id}")
    public String supplier(@PathVariable long id) {
        throw new SupplierNotFoundException(id);
    }

    @GetMapping("/crash")
    public String crash() {
        throw new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'");
    }
}
