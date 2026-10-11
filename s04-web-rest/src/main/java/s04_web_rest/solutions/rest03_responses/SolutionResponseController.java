package s04_web_rest.solutions.rest03_responses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s04_web_rest.rest03_responses.ResponseExerciseController.Coupon;
import s04_web_rest.rest03_responses.ResponseExerciseController.NewReview;
import s04_web_rest.rest03_responses.ResponseExerciseController.Review;
import s04_web_rest.rest03_responses.ResponseExerciseController.Stock;
import s04_web_rest.rest03_responses.ResponseExerciseController.StockUpdate;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Rozwiązania wzorcowe ćwiczeń 1–3 lekcji Rest03Responses — zajrzyj dopiero po własnej próbie! */
@RestController
public class SolutionResponseController {

    static final Map<String, Integer> COUPONS = Map.of("LATO10", 10, "ZIMA20", 20);

    final Map<Long, Review> reviews = new ConcurrentHashMap<>();
    final AtomicLong nextReviewId = new AtomicLong(1);
    final Map<Long, Integer> stock = new ConcurrentHashMap<>();

    @GetMapping("/api/exercise/coupons/{code}")
    public ResponseEntity<Coupon> coupon(@PathVariable String code) {
        return ResponseEntity.of(Optional.ofNullable(COUPONS.get(code)).map(percent -> new Coupon(code, percent)));
    }

    @PostMapping("/api/exercise/reviews")
    public ResponseEntity<Review> addReview(@RequestBody NewReview newReview) {
        if (newReview.stars() < 1 || newReview.stars() > 5) {
            return ResponseEntity.badRequest().build();
        }
        long id = nextReviewId.getAndIncrement();
        Review review = new Review(id, newReview.productId(), newReview.stars(), newReview.text());
        reviews.put(id, review);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(review);
    }

    @PutMapping("/api/exercise/stock/{productId}")
    public ResponseEntity<Stock> putStock(@PathVariable long productId, @RequestBody StockUpdate update) {
        if (update.quantity() < 0) {
            return ResponseEntity.badRequest().build();
        }
        Integer previous = stock.put(productId, update.quantity());   // null = wcześniej nie było
        Stock body = new Stock(productId, update.quantity());
        if (previous == null) {
            URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
            return ResponseEntity.created(location).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/exercise/stock/{productId}")
    public ResponseEntity<Stock> getStock(@PathVariable long productId) {
        Integer quantity = stock.get(productId);
        return quantity == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(new Stock(productId, quantity));
    }
}
