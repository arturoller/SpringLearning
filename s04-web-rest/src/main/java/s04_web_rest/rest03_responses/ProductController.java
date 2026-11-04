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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/** Sekcje 1–5 lekcji Rest03Responses: różne sposoby ustawiania statusu, nagłówków i ciała odpowiedzi. */
@RestController
@RequestMapping("/rest03/products")
public class ProductController {

    /** Dane do utworzenia produktu (to, co przysyła klient). */
    public record NewProduct(String name, long priceGrosze) {
    }

    private final ProductStore store;

    public ProductController(ProductStore store) {
        this.store = store;
    }

    // ------------------------------------------------------------------ sekcja 1: ResponseEntity

    /** 200 OK z ciałem albo 404 Not Found bez ciała. */
    @GetMapping("/{id}")
    public ResponseEntity<Product> byId(@PathVariable long id) {
        return store.find(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 200 OK + własny nagłówek X-Total-Count z liczbą produktów (sekcja 4). */
    @GetMapping
    public ResponseEntity<List<Product>> list() {
        List<Product> all = store.all();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(all.size()))
                .body(all);
    }

    /**
     * Tworzenie: 201 Created + nagłówek Location (adres nowego zasobu) + ciało z utworzonym produktem.
     * Błędy: 400 (brak nazwy) i 409 (produkt o tej nazwie już jest). UriComponentsBuilder jako parametr metody Spring
     * wstrzykuje już ustawiony na adres bieżącego serwera (schemat, host, port), więc nie wpisujesz adresu ręcznie.
     */
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody NewProduct request, UriComponentsBuilder uriBuilder) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (store.existsByName(request.name())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Product created = store.add(request.name(), request.priceGrosze());
        URI location = uriBuilder.path("/rest03/products/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** 204 No Content po udanym usunięciu (usunięto, nie ma co odsyłać) albo 404, gdy produktu nie było. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        return store.remove(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ------------------------------------------------------------------ sekcja 2: @ResponseStatus

    /** Status 201 ustawiony adnotacją — bez ResponseEntity, ale też bez nagłówka Location. */
    @PostMapping("/quick")
    @ResponseStatus(HttpStatus.CREATED)
    public Product quick(@RequestBody NewProduct request) {
        return store.add(request.name(), request.priceGrosze());
    }

    /** PUŁAPKA: @ResponseStatus przy zwracanym ResponseEntity — status z ResponseEntity wygrywa (200), adnotacja nic nie zmienia. */
    @GetMapping("/{id}/both")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Product> both(@PathVariable long id) {
        return store.find(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Wyjątek z @ResponseStatus: id 2 (Herbata) jest w tym przykładzie „wyprzedana” → 409. */
    @PostMapping("/{id}/reserve")
    public Product reserve(@PathVariable long id) {
        Product product = store.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produkt " + id + " nie istnieje"));
        if (id == 2) {
            throw new SoldOutException(id);
        }
        return product;
    }

    // ------------------------------------------------------------------ sekcja 3: ResponseStatusException

    /** Szybki błąd z kodem i opisem, bez własnej klasy wyjątku: 404 z powodem „Produkt … nie istnieje”. */
    @GetMapping("/{id}/or-fail")
    public Product byIdOrFail(@PathVariable long id) {
        return store.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produkt " + id + " nie istnieje"));
    }

    // ------------------------------------------------------------------ sekcja 4: nagłówki odpowiedzi

    /** Pamięć podręczna: Cache-Control (jak długo wolno trzymać odpowiedź) i ETag (odcisk wersji zasobu). */
    @GetMapping("/{id}/cached")
    public ResponseEntity<Product> cached(@PathVariable long id) {
        return store.find(id)
                .map(product -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                        .eTag("\"v1-" + product.id() + "\"")
                        .body(product))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ------------------------------------------------------------------ sekcja 5: pułapka null

    /**
     * PUŁAPKA: gdy produktu nie ma, zwracamy null. Spring NIE zamieni tego na 404 — odpowiedź to 200 OK z pustym ciałem.
     */
    @GetMapping("/{id}/maybe")
    public Product maybe(@PathVariable long id) {
        return store.find(id).orElse(null);
    }
}
