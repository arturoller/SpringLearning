package s12_security.sec05_jwt;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * CatalogController = API chronione tokenem JWT (Resource Server). Reguły w Sec05JwtConfig.resourceServerChain:
 * GET produktów — SCOPE_products:read, POST — SCOPE_products:write, /api/admin/** — rola ADMIN.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    /** Produkt: numer i nazwa. */
    public record Product(String sku, String name) {
    }

    @GetMapping("/products")
    public List<Product> products() {
        return List.of(new Product("KSI-002", "Java. Podstawy"), new Product("KSI-003", "Wzorce projektowe"));
    }

    @PostMapping("/products")
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.created(URI.create("/api/products/" + product.sku())).body(product);
    }

    @GetMapping("/admin/report")
    public Map<String, Object> report() {
        return Map.of("report", "Sprzedaż dzienna", "ordersToday", 12);
    }

    /**
     * Kim jestem według tokenu? Principal to teraz obiekt {@code Jwt} (zdekodowany token),
     * a Authentication — JwtAuthenticationToken z uprawnieniami wyliczonymi z claimów.
     */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).sorted().toList();
        return Map.of(
                "subject", jwt.getSubject(),
                "issuer", jwt.getIssuer().toString(),   // getIssuer() zwraca URL — iss MUSI być adresem (Sec05Jwt, sekcja 4)
                "scope", jwt.getClaimAsString("scope"),
                "authorities", authorities,
                "authenticationType", authentication.getClass().getSimpleName());
    }
}
