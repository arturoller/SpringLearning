package s06_error_handling.err01_default_errors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Err01Controller = kontroler z celowo zepsutymi punktami końcowymi (endpoint) — pokazują, co Spring Boot robi
 * z błędami, gdy NIE napiszemy żadnej własnej obsługi.
 */
@RestController
public class Err01Controller {

    /**
     * Wyjątek środowiska uruchomieniowego (RuntimeException) — nikt go nie łapie. Komunikat celowo zawiera
     * "wrażliwe" dane (adres bazy, użytkownik), żeby pokazać, czemu nie wolno ich wysyłać klientowi.
     */
    @GetMapping("/api/boom")
    public String boom() {
        throw new IllegalStateException("Brak połączenia z bazą jdbc:postgresql://db.intranet:5432/shop (użytkownik shop_user)");
    }

    /** Poprawny punkt końcowy; {@code /api/products/abc} da błąd 400, bo "abc" nie jest liczbą. */
    @GetMapping("/api/products/{id}")
    public Map<String, Object> product(@PathVariable long id) {
        return Map.of("id", id, "name", "Kawa");
    }
}
