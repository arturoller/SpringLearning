package s06_error_handling.err01_default_errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Kontroler lekcji Err01DefaultErrors: każdy punkt końcowy (endpoint) kończy się błędem na inny sposób, a obsługę błędów
 * zostawiamy DOMYŚLNĄ — Boot zrobi z tego odpowiedź przez {@code /error}.
 */
@RestController
@RequestMapping("/err01")
public class ShopErrorsController {

    /** Wyjątek bez żadnej obsługi → 500. Komunikat zawiera "tajne" szczegóły techniczne, które NIE mogą trafić do klienta. */
    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("Błąd wewnętrzny: połączenie z bazą db-prod-01 odrzucone");
    }

    /** Własny wyjątek z {@code @ResponseStatus(CONFLICT)} → 409. */
    @GetMapping("/stock/{sku}")
    public String stock(@PathVariable String sku) {
        throw new OutOfStockException(sku);
    }

    /** {@code ResponseStatusException} — status wybierany w miejscu rzucenia: 404 z opisem. */
    @GetMapping("/products/{id}")
    public String product(@PathVariable long id) {
        if (id != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu o id " + id);
        }
        return "Kawa";
    }
}
