package s15_actuator_logging.obs01_actuator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Zwykły kontroler aplikacji (punkt końcowy biznesowy — nie Actuatora). Actuator dokłada OBOK niego własne adresy
 * pod /actuator, a my nie piszemy dla nich ani jednego kontrolera.
 */
@RestController   // rest controller = kontroler REST (zwraca JSON, nie widok)
@RequestMapping("/api/products")   // request mapping = mapowanie zapytań (wspólny początek adresu)
public class ProductController {

    private static final List<Product> PRODUCTS = List.of(
            new Product("ELE-001", "Laptop Pro 14", "ELEKTRONIKA", new BigDecimal("5499.99"), 7),
            new Product("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", new BigDecimal("64.99"), 120),
            new Product("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00"), 15));

    @GetMapping   // get mapping = mapowanie metody GET
    public List<Product> all() {
        return PRODUCTS;
    }
}
