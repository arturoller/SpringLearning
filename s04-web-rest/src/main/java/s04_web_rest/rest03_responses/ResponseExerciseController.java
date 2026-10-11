package s04_web_rest.rest03_responses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ResponseExerciseController = kontroler ćwiczeń lekcji Rest03Responses — SZKIELET. Adresy i parametry są gotowe;
 * uzupełnij ciała tak, żeby zwracały właściwe kody statusu. Test: Rest03ResponsesExercisesTest.
 */
@RestController
public class ResponseExerciseController {

    /** Coupon = kupon rabatowy: kod i procent rabatu. */
    public record Coupon(String code, int percent) {
    }

    /** NewReview = nowa opinia o produkcie (ciało POST): gwiazdki 1..5 i tekst. */
    public record NewReview(long productId, int stars, String text) {
    }

    /** Review = zapisana opinia z id nadanym przez serwer. */
    public record Review(long id, long productId, int stars, String text) {
    }

    /** StockUpdate = nowy stan magazynu w ciele PUT: {"quantity":12}. */
    public record StockUpdate(int quantity) {
    }

    /** Stock = stan magazynowy produktu. */
    public record Stock(long productId, int quantity) {
    }

    /** Kupony sklepu: kod → procent rabatu. */
    static final Map<String, Integer> COUPONS = Map.of("LATO10", 10, "ZIMA20", 20);

    /** Opinie (id → opinia) i licznik id — ConcurrentHashMap/AtomicLong są bezpieczne dla wielu wątków. */
    final Map<Long, Review> reviews = new ConcurrentHashMap<>();
    final AtomicLong nextReviewId = new AtomicLong(1);

    /** Stany magazynowe: id produktu → ilość. */
    final Map<Long, Integer> stock = new ConcurrentHashMap<>();

    /**
     * ĆWICZENIE 1 (łatwe): {@code GET /api/exercise/coupons/LATO10} → 200 i {"code":"LATO10","percent":10};
     * nieznany kod → 404 bez ciała. Podpowiedź: {@code ResponseEntity.of(Optional.ofNullable(COUPONS.get(code)).map(...))}.
     */
    @GetMapping("/api/exercise/coupons/{code}")
    public ResponseEntity<Coupon> coupon(@PathVariable String code) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): {@code POST /api/exercise/reviews} → zapisz opinię z kolejnym id (nextReviewId) i odpowiedz
     * 201 Created, nagłówkiem Location z pełnym adresem nowej opinii (…/api/exercise/reviews/{id}) i opinią w ciele.
     * Gwiazdki spoza 1..5 → 400 Bad Request bez ciała (nic nie zapisuj).
     * Podpowiedź: ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()
     * i {@code ResponseEntity.created(uri).body(...)}; {@code ResponseEntity.badRequest().build()}.
     */
    @PostMapping("/api/exercise/reviews")
    public ResponseEntity<Review> addReview(@RequestBody NewReview newReview) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): {@code PUT /api/exercise/stock/{productId}} z ciałem {"quantity":12} ustawia stan
     * magazynu. Gdy produktu w mapie stock jeszcze NIE BYŁO → 201 Created z nagłówkiem Location (pełny adres tego
     * zapytania) i Stock w ciele; gdy BYŁ → 200 OK i Stock w ciele; ilość ujemna → 400 bez zmian w mapie.
     * PUT jest idempotentny: drugi taki sam PUT da 200 i ten sam stan. Podpowiedź: {@code stock.put(...)} zwraca
     * POPRZEDNIĄ wartość (null, gdy klucza nie było); adres — ServletUriComponentsBuilder.fromCurrentRequest().build().toUri().
     */
    @PutMapping("/api/exercise/stock/{productId}")
    public ResponseEntity<Stock> putStock(@PathVariable long productId, @RequestBody StockUpdate update) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Gotowe (nie jest częścią ćwiczenia): odczyt stanu magazynu — 200 albo 404. */
    @GetMapping("/api/exercise/stock/{productId}")
    public ResponseEntity<Stock> getStock(@PathVariable long productId) {
        Integer quantity = stock.get(productId);
        return quantity == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(new Stock(productId, quantity));
    }
}
