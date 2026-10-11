package s06_error_handling.err05_custom_exceptions;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler zamówień: cienki — waliduje wejście ({@code @Valid}), woła serwis i NIE obsługuje wyjątków. */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderConfirmation place(@Valid @RequestBody OrderRequest request) {
        return orderService.place(request);
    }
}
