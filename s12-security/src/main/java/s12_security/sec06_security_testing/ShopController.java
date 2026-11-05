package s12_security.sec06_security_testing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * API sklepu lekcji Sec06SecurityTesting ({@code /sec06/**}, łańcuch {@code apiChain}: token JWT). Reguły:
 * {@link Sec06SecurityConfig} (adresy) i {@link OrderService} (metody).
 */
@RestController
@RequestMapping("/sec06")
public class ShopController {

    private final OrderService orders;

    public ShopController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/products")
    public List<String> products() {
        return List.of("Kawa ziarnista", "Herbata zielona");
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public String addProduct() {
        return "Dodano produkt";
    }

    @GetMapping("/orders")
    public List<OrderService.ShopOrder> myOrders(Principal principal) {
        return orders.ordersOf(principal.getName());
    }

    @GetMapping("/orders/of/{username}")
    public List<OrderService.ShopOrder> ordersOf(@PathVariable String username) {
        return orders.ordersOf(username);
    }

    @DeleteMapping("/orders/{id}")
    public String cancel(@PathVariable long id) {
        return orders.cancel(id);
    }

    @GetMapping("/admin/stats")
    public String adminStats() {
        return "Liczba zamówień: 3";
    }
}
