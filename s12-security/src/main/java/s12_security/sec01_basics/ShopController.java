package s12_security.sec01_basics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * ShopController = kontroler sklepu z trzema „poziomami dostępu”:
 * <ul>
 *   <li>{@code GET /api/public/products} — lista produktów: ma być dostępna dla KAŻDEGO (także niezalogowanego),</li>
 *   <li>{@code GET /api/orders} — zamówienia: tylko dla zalogowanych,</li>
 *   <li>{@code GET /api/admin/report} — raport sprzedaży: tylko dla roli ADMIN.</li>
 * </ul>
 * Sam kontroler NIC nie wie o zabezpieczeniach — reguły są w łańcuchu filtrów (Sec01SecurityConfig). Dzięki temu
 * ten sam kontroler działa raz „pod samym starterem” (Sec01StarterOnlyApplication), a raz z własnymi regułami
 * (Sec01Application).
 */
@RestController
@RequestMapping("/api")
public class ShopController {

    /** Produkty — dane jawne. */
    @GetMapping("/public/products")
    public List<String> products() {
        return List.of("ELE-001 Laptop Pro 14", "SPO-001 Kawa ziarnista 1kg", "KSI-001 Czysty kod");
    }

    /**
     * Zamówienia zalogowanego użytkownika. {@code Principal} (podmiot — „kto pyta”) Spring MVC wstrzykuje z kontekstu
     * bezpieczeństwa; dla niezalogowanego byłby null, ale tu niezalogowany w ogóle nie dojdzie (zatrzyma go filtr).
     */
    @GetMapping("/orders")
    public Map<String, Object> orders(Principal principal) {
        return Map.of("user", principal.getName(), "orders", List.of("ZAM-001", "ZAM-002"));
    }

    /** Raport — tylko dla administratora. */
    @GetMapping("/admin/report")
    public Map<String, Object> report() {
        return Map.of("report", "Sprzedaż dzienna", "ordersToday", 12);
    }
}
