package s06_error_handling.solutions.err01_default_errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err01DefaultErrors — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/err01/cw")
public class Err01SolutionsController {

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class NoStockException extends RuntimeException {

        public NoStockException(String sku) {
            super("Brak towaru: " + sku);
        }
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable long id) {
        if (id == 1 || id == 2) {
            return "Zamówienie #" + id;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma zamówienia " + id);
    }

    @GetMapping("/stock/{sku}")
    public String stock(@PathVariable String sku) {
        if (sku.equals("kawa")) {
            throw new NoStockException(sku);
        }
        return "Jest: " + sku;
    }

    @GetMapping("/discount")
    public String discount(@RequestParam int percent) {
        if (percent < 0 || percent > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rabat musi być w przedziale 0-100");
        }
        return "Rabat: " + percent + "%";
    }

    public static String summarize(Map<String, Object> errorBody) {
        String summary = errorBody.get("status") + " " + errorBody.get("error") + " (" + errorBody.get("path") + ")";
        Object message = errorBody.get("message");
        if (message != null && !message.toString().isEmpty()) {
            summary += ": " + message;
        }
        return summary;
    }

    public static List<String> leakedKeys(Map<String, Object> errorBody) {
        Set<String> always = Set.of("trace", "exception", "errors");
        return errorBody.entrySet().stream()
                .filter(entry -> always.contains(entry.getKey())
                        || (entry.getKey().equals("message")
                        && (String.valueOf(entry.getValue()).contains("db-") || String.valueOf(entry.getValue()).contains("SELECT"))))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
