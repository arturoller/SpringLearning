package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Kontroler bez ŻADNEJ własnej obsługi błędów — wszystkim zajmują się advice. */
@RestController
public class ProductController {

    @GetMapping("/api/products/{id}")
    public Map<String, Object> product(@PathVariable long id) {
        if (id == 999) {
            throw new ProductNotFoundException(id);
        }
        return Map.of("id", id, "name", "Kawa");
    }

    @GetMapping("/api/products/pay")
    public String pay() {
        throw new PaymentDeclinedException("Bank odrzucił kartę 4111-XXXX");
    }

    @GetMapping("/api/products/conflict")
    public String conflict() {
        throw new IllegalStateException("Produkt jest zablokowany przez inną transakcję");
    }

    /** Wyjątek sprawdzany (checked) — łapie go dopiero catch-all {@code Exception} z GlobalExceptionHandler. */
    @GetMapping("/api/products/crash")
    public String crash() throws Exception {
        throw new Exception("Nieprzewidziana awaria w module cen (tabela PRICE_V3)");
    }
}
