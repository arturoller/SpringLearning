package s06_error_handling.err02_exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Magazyn — kontroler z DWIOMA metodami obsługi: wąską (IllegalArgumentException) i szeroką (RuntimeException).
 * Pokazuje, który handler wygrywa i jaka pułapka czeka przy zbyt szerokim handlerze.
 */
@RestController
public class InventoryController {

    /** {@code Integer.parseInt("abc")} rzuca NumberFormatException — podklasę IllegalArgumentException. */
    @GetMapping("/api/inventory/parse")
    public Map<String, Object> parse(@RequestParam String value) {
        return Map.of("value", Integer.parseInt(value));
    }

    /** Błąd programisty: IllegalStateException pasuje tylko do szerokiego handlera RuntimeException. */
    @GetMapping("/api/inventory/broken")
    public String broken() {
        throw new IllegalStateException("Wewnętrzny stan magazynu jest uszkodzony (tabela STOCK_V2)");
    }

    /** Wyjątek z {@code @ResponseStatus(404)} — szeroki handler RuntimeException "połknie" go i zrobi z niego 500! */
    @GetMapping("/api/inventory/missing")
    public String missing() {
        throw new ProductNotFoundException(999);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badArgument(IllegalArgumentException e) {
        return Map.of("error", "Niepoprawne dane: " + e.getMessage());
    }

    /** Szeroki handler: 500 i ogólny komunikat bez szczegółów wewnętrznych. */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> anyRuntime(RuntimeException e) {
        return Map.of("error", "Wystąpił nieoczekiwany błąd");
    }
}
