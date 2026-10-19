package s22_extras.ext03_oauth2_keycloak;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API sklepu lekcji Ext03OAuth2Keycloak: część publiczna, część dla klienta, część dla pracownika. */
@RestController
@RequestMapping("/api")
public class ShopApiController {

    /** Kim jestem według tokenu: nazwa, e-mail z claimu, uprawnienia (posortowane). */
    public record Me(String name, String email, List<String> authorities) {
    }

    @GetMapping("/public/products")
    public List<String> products() {
        return List.of("ELE-001 Laptop Pro 14", "SPO-001 Kawa ziarnista 1kg", "KSI-001 Czysty kod");
    }

    @GetMapping("/me")
    public Me me(JwtAuthenticationToken authentication, @AuthenticationPrincipal Jwt jwt) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return new Me(authentication.getName(), jwt.getClaimAsString("email"), authorities);
    }

    @GetMapping("/orders/my")
    public List<String> myOrders(@AuthenticationPrincipal Jwt jwt) {
        return List.of("ZAM-001 (" + jwt.getClaimAsString("preferred_username") + ")");
    }

    @GetMapping("/admin/orders")
    public List<String> allOrders() {
        return List.of("ZAM-001 NOWE", "ZAM-002 OPLACONE", "ZAM-003 WYSLANE");
    }
}
