package s06_error_handling.err05_custom_exceptions;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Cienki kontroler: deleguje do serwisu i niczego nie obsługuje — błędy zamienia na odpowiedzi ShopProblemAdvice. */
@RestController
public class ShopController {

    private final OrderService orders;

    public ShopController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/api/products/{sku}")
    public Map<String, Object> product(@PathVariable String sku) {
        return Map.of("sku", sku, "stock", orders.stockOf(sku));
    }

    @PostMapping("/api/orders")
    public Map<String, String> placeOrder(@Valid @RequestBody NewOrder order) {
        return Map.of("id", orders.place(order));
    }

    @GetMapping("/api/reports/sales")
    public int salesReport() {
        return orders.salesReport();
    }

    @GetMapping("/api/bug")
    public String bug() {
        orders.bug();
        return "nie dojdzie";
    }
}
