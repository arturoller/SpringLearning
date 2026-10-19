package s12_security.sec02_authorization;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * OrderController = zamówienia. Kontroler nie sprawdza uprawnień — robi to OrderService ({@code @PreAuthorize},
 * {@code @PostAuthorize}). Odmowa w serwisie kończy się wyjątkiem AuthorizationDeniedException, który
 * ExceptionTranslationFilter zamienia na 403.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/{owner}")
    public List<OrderService.Order> ordersOf(@PathVariable String owner) {
        return orders.ordersOf(owner);
    }

    @GetMapping("/by-id/{id}")
    public OrderService.Order byId(@PathVariable String id) {
        return orders.findById(id);
    }

    @GetMapping("/{owner}/summary")
    public String summary(@PathVariable String owner) {
        return orders.summaryOf(owner);
    }
}
