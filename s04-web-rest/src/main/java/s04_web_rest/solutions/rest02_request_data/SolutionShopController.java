package s04_web_rest.solutions.rest02_request_data;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s04_web_rest.rest02_request_data.ShopExerciseController.CartLine;
import s04_web_rest.rest02_request_data.ShopExerciseController.CartRequest;
import s04_web_rest.rest02_request_data.ShopExerciseController.CartTotal;
import s04_web_rest.rest02_request_data.ShopExerciseController.Greeting;
import s04_web_rest.rest02_request_data.ShopExerciseController.Listing;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń 1–3 lekcji Rest02RequestData — zajrzyj dopiero po własnej próbie! */
@RestController
public class SolutionShopController {

    @GetMapping("/api/exercise/greeting")
    public Greeting greeting(@RequestParam(defaultValue = "gościu") String name) {
        return new Greeting("Cześć, " + name + "!");
    }

    @GetMapping("/api/exercise/categories/{category}/products")
    public Listing listing(@PathVariable String category,
                           @RequestParam(name = "tag", required = false) List<String> tags,
                           @RequestParam(defaultValue = "10") int limit) {
        return new Listing(category, tags == null ? List.of() : tags, limit);
    }

    @PostMapping("/api/exercise/cart/total")
    public CartTotal cartTotal(@RequestBody CartRequest cart,
                               @RequestHeader(name = "X-Discount-Percent", defaultValue = "0") int discount) {
        if (discount < 0 || discount > 100) {
            throw new IllegalArgumentException("Rabat musi być w zakresie 0..100: " + discount);
        }
        if (cart.lines() == null || cart.lines().isEmpty()) {
            throw new IllegalArgumentException("Koszyk jest pusty");
        }
        int sum = cart.lines().stream().mapToInt(SolutionShopController::lineValue).sum();
        return new CartTotal((sum * (100 - discount) + 50) / 100, discount);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Void> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }

    /** Wartość jednej pozycji koszyka w groszach: cena × ilość. */
    static int lineValue(CartLine line) {
        return line.priceGrosze() * line.quantity();
    }
}
