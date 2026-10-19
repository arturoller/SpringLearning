package s22_extras.ext01_spring_ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punkty końcowe (endpointy) lekcji Ext01SpringAi — do sprawdzania ręcznego z s22-extras/http/ext01_spring_ai.http
 * (z uruchomioną Ollamą) i w teście (z atrapą modelu).
 */
@RestController
@RequestMapping("/api/ai")
public class ProductAiController {

    /** Odpowiedź z opisem: sku produktu i tekst wygenerowany przez model. */
    public record DescriptionResponse(String sku, String text) {
    }

    /** Zapytanie z opinią klienta. */
    public record ReviewRequest(String text) {
    }

    /** Odpowiedź z wydźwiękiem opinii. */
    public record SentimentResponse(ReviewSentiment sentiment) {
    }

    private final ProductCatalog catalog;
    private final ProductCopywriter copywriter;
    private final ReviewClassifier classifier;

    public ProductAiController(ProductCatalog catalog, ProductCopywriter copywriter, ReviewClassifier classifier) {
        this.catalog = catalog;
        this.copywriter = copywriter;
        this.classifier = classifier;
    }

    @GetMapping("/products/{sku}/description")
    public ResponseEntity<DescriptionResponse> description(@PathVariable String sku) {
        return catalog.find(sku)
                .map(product -> ResponseEntity.ok(new DescriptionResponse(sku, copywriter.shortDescription(product))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/{sku}/card")
    public ResponseEntity<ProductCard> card(@PathVariable String sku) {
        return catalog.find(sku)
                .map(product -> ResponseEntity.ok(copywriter.card(product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reviews/sentiment")
    public SentimentResponse sentiment(@RequestBody ReviewRequest request) {
        return new SentimentResponse(classifier.classify(request.text()));
    }
}
