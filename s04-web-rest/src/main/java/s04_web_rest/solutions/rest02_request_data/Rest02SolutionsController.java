package s04_web_rest.solutions.rest02_request_data;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest02RequestData — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/rest02/cw")
public class Rest02SolutionsController {

    public record Coupon(String code, Integer percent) {
    }

    /** Rozwiązanie 1: dwie zmienne ścieżki, każda z własnym @PathVariable. */
    @GetMapping("/customers/{customerId}/orders/{orderId}")
    public String order(@PathVariable Long customerId, @PathVariable Long orderId) {
        return "klient " + customerId + ", zamówienie " + orderId;
    }

    /** Rozwiązanie 2: defaultValue czyni parametr opcjonalnym. */
    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "gościu") String name) {
        return "Cześć, " + name + "!";
    }

    /** Rozwiązanie 3: nagłówek z myślnikiem → nazwa podana jawnie; wymagany domyślnie → brak = 400. */
    @GetMapping("/lang")
    public String lang(@RequestHeader("X-Lang") String language) {
        return "język: " + language;
    }

    /** Rozwiązanie 4: @RequestBody zamienia JSON na rekord. */
    @PostMapping("/coupon")
    public String coupon(@RequestBody Coupon coupon) {
        return "kod " + coupon.code() + ": -" + coupon.percent() + "%";
    }

    /** Rozwiązanie 5: lista parametrów; konwersja na Integer robi Spring (zły typ → 400). */
    @GetMapping("/sum")
    public String sum(@RequestParam List<Integer> n) {
        return String.valueOf(n.stream().mapToInt(Integer::intValue).sum());
    }
}
