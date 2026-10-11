package s04_web_rest.rest03_responses;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ProductResponseController = kontroler pokazujący różne odpowiedzi — sekcje 1–5 lekcji Rest03Responses.
 */
@RestController
@RequestMapping("/api/products")
public class ProductResponseController {

    private final ProductStore store;

    public ProductResponseController(ProductStore store) {
        this.store = store;
    }

    /** Sekcja 1. Zwykły obiekt → zawsze 200 OK. Spring sam opakowuje wynik w odpowiedź. */
    @GetMapping("/plain/{id}")
    public Product plain(@PathVariable long id) {
        return store.findById(id).orElse(null);   // ZŁY PRZYKŁAD przy braku: null → 200 z pustym ciałem
    }

    /** Sekcja 2. {@code ResponseEntity.of(Optional)} — jest produkt → 200 z ciałem; brak → 404 bez ciała. */
    @GetMapping("/{id}")
    public ResponseEntity<Product> one(@PathVariable long id) {
        return ResponseEntity.of(store.findById(id));
    }

    /**
     * Sekcja 2. To samo „ręcznie”: map(ResponseEntity::ok) + orElseGet(notFound) — gdy chcesz np. dodać nagłówek
     * tylko do odpowiedzi 200.
     */
    @GetMapping("/manual/{id}")
    public ResponseEntity<Product> oneManual(@PathVariable long id) {
        return store.findById(id)
                .map(product -> ResponseEntity.ok().header("X-Product-Name-Length",
                        String.valueOf(product.name().length())).body(product))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Sekcja 3. POST → 201 Created + nagłówek Location z PEŁNYM adresem nowego zasobu, zbudowanym z adresu
     * bieżącego zapytania: http://host:port/api/products + /{id}.
     */
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody NewProduct newProduct) {
        Product created = store.add(newProduct);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()   // adres bieżącego zapytania
                .path("/{id}")                                             // + /{id}
                .buildAndExpand(created.id())                              // {id} → 4
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Sekcja 3. DELETE → 204 No Content (sukces, ale bez ciała) albo 404, gdy nie było czego usuwać. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return store.remove(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Sekcja 4. {@code @ResponseStatus} na METODZIE — zwykły obiekt, ale status 201 zamiast 200. Bez nagłówka Location! */
    @PostMapping("/quick")
    @ResponseStatus(HttpStatus.CREATED)
    public Product createQuick(@RequestBody NewProduct newProduct) {
        return store.add(newProduct);
    }

    /** Sekcja 4. {@code @ResponseStatus} na KLASIE WYJĄTKU — rzucony ProductNotFoundException daje 404. */
    @GetMapping("/{id}/details")
    public Product details(@PathVariable long id) {
        return store.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    /** Sekcja 5. Własne nagłówki: liczba produktów (X-Total-Count) i Cache-Control (jak długo wolno pamiętać wynik). */
    @GetMapping
    public ResponseEntity<List<Product>> all() {
        List<Product> products = store.findAll();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(products.size()))
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                .body(products);
    }

    /** Sekcja 5. Dowolny status z HttpStatus — tu 202 Accepted: „przyjęte, przetworzę później”. */
    @PostMapping("/import")
    public ResponseEntity<String> importProducts() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Import przyjęty do realizacji");
    }

    /** Pytanie kontrolne 5: {@code @ResponseStatus} na metodzie ORAZ ResponseEntity z innym statusem — kto wygra? */
    @GetMapping("/conflict")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> conflictingStatus() {
        return ResponseEntity.ok("który status?");
    }
}
