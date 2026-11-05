package s12_security.sec02_authorization;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Zamówienia lekcji Sec02Authorization. Reguła adresu wpuszcza każdego klienta i pracownika, a to, CZYJE zamówienia zobaczy,
 * rozstrzyga {@link OrderService} (bezpieczeństwo metod, sekcja 3).
 */
@RestController
@RequestMapping("/sec02/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<ShopOrder> visible() {
        return orders.visibleOrders();
    }

    @GetMapping("/of/{username}")
    public List<ShopOrder> of(@PathVariable String username) {
        return orders.ordersOf(username);
    }

    @GetMapping("/{id}")
    public ShopOrder byId(@PathVariable long id) {
        return orders.byId(id);
    }
}
