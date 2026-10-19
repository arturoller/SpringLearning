package s12_security.sec06_security_testing;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * ShopController = punkty końcowe, dla których w lekcji Sec06SecurityTesting piszemy testy 401/403/200
 * „dla każdej reguły” z Sec06SecurityConfig.
 */
@RestController
@RequestMapping("/api")
public class ShopController {

    /** Produkt: numer i nazwa. */
    public record Product(String sku, String name) {
    }

    /** Odpowiedź „kim jestem” — DTO, bez hasła. */
    public record Me(String username, List<String> authorities) {
    }

    private final OrderService orders;

    public ShopController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/products")
    public List<Product> products() {
        return List.of(new Product("ODZ-001", "Kurtka zimowa"), new Product("ODZ-002", "T-shirt bawełniany"));
    }

    @PostMapping("/products")
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.created(URI.create("/api/products/" + product.sku())).body(product);
    }

    @GetMapping("/orders/{owner}")
    public List<String> orders(@PathVariable String owner) {
        return orders.ordersOf(owner);
    }

    @GetMapping("/me")
    public Me me(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return new Me(authentication.getName(), authorities);
    }

    /** Stan magazynu dla partnera z tokenem JWT (scope stock:read). */
    @GetMapping("/partner/stock")
    public Map<String, Object> stock(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("partner", jwt.getSubject(), "ODZ-001", 12, "ODZ-002", 80);
    }
}
