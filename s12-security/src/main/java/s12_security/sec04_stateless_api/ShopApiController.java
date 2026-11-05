package s12_security.sec04_stateless_api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * Bezstanowe API sklepu lekcji Sec04StatelessApi ({@code /sec04/api/**}). Każde żądanie przynosi dane logowania w nagłówku
 * {@code Authorization} — serwer niczego nie pamięta między żądaniami (sekcja 1). Reguły dostępu: {@link Sec04SecurityConfig}.
 */
@RestController
@RequestMapping("/sec04/api")
public class ShopApiController {

    /** Produkt w katalogu. */
    public record Product(long id, String name, BigDecimal price) {
    }

    /** Dane nowego produktu z ciała żądania. */
    public record NewProduct(String name, BigDecimal price) {
    }

    private static final List<Product> CATALOG = List.of(
            new Product(1, "Kawa ziarnista", new BigDecimal("39.90")),
            new Product(2, "Herbata zielona", new BigDecimal("18.50")));

    @GetMapping("/products")
    public List<Product> products() {
        return CATALOG;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody NewProduct request) {
        return new Product(3, request.name(), request.price());    // "na niby" — nic nie zapisujemy
    }

    @GetMapping("/orders")
    public String orders(Principal principal) {
        return "Zamówienia użytkownika " + principal.getName();
    }

    @GetMapping("/admin/stats")
    public String adminStats() {
        return "Liczba zamówień: 3";
    }
}
