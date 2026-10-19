package s14_openapi.api03_groups_and_versions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Administracyjne punkty końcowe sklepu — trafiają do osobnej grupy dokumentacji „admin”. */
@RestController
@RequestMapping("/admin")
public class AdminController {

    /** Statystyki sklepu. */
    public record Stats(int products, int customers) {
    }

    /** Wynik uzupełnienia magazynu. */
    public record Restock(String sku, int added) {
    }

    @GetMapping("/stats")
    public Stats stats() {
        return new Stats(14, 7);
    }

    @PostMapping("/products/{sku}/restock")
    public Restock restock(@PathVariable String sku, @RequestParam int quantity) {
        return new Restock(sku, quantity);
    }
}
