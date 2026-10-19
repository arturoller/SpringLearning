package s04_web_rest.rest03_responses;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductController = kontroler pokazujący, jak SKŁADAĆ ODPOWIEDŹ: status, nagłówki i ciało.
 * {@code ResponseEntity} (encja odpowiedzi) — cała odpowiedź HTTP jako obiekt: status + nagłówki + ciało.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public ProductController() {
        save(new Product("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"), 7));
        save(new Product("KSI-001", "Czysty kod", new BigDecimal("79.00"), 15));
        save(new Product("SPO-002", "Czekolada gorzka", new BigDecimal("7.49"), 300));
    }

    // ---------------------------------------------------------------- 1. ResponseEntity: status, nagłówki, ciało

    /** GET /api/products → 200, nagłówek X-Total-Count (liczba wszystkich produktów) i lista w ciele. */
    @GetMapping
    public ResponseEntity<List<Product>> all() {
        List<Product> list = products.values().stream().sorted(Comparator.comparing(Product::sku)).toList();
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(list.size()))
                .body(list);
    }

    /**
     * GET /api/products/ELE-001 (Accept: application/json) → 200 i JSON; brak → 404 z PUSTYM ciałem.
     * {@code ResponseEntity.of(Optional)} — skrót: Optional z wartością → 200 + ciało, pusty → 404.
     * produces (produkuje) = jaki typ zawartości zwraca TA metoda — Spring wybiera ją, gdy klient akceptuje JSON.
     */
    @GetMapping(value = "/{sku}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> one(@PathVariable String sku) {
        return ResponseEntity.of(find(sku));
    }

    /**
     * Ten SAM adres, inny typ odpowiedzi: GET /api/products/ELE-001 z nagłówkiem Accept: text/plain → zwykły tekst.
     * To negocjacja treści (content negotiation): klient mówi w Accept, co woli, a Spring dobiera metodę po produces.
     */
    @GetMapping(value = "/{sku}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> oneAsText(@PathVariable String sku) {
        return find(sku)
                .map(product -> ResponseEntity.ok(product.name() + " — " + product.price() + " zł"))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------- 2. 201 Created + Location

    /**
     * POST /api/products z JSON-em → 201 Created, nagłówek Location z PEŁNYM adresem nowego zasobu i produkt w ciele.
     * consumes (konsumuje) = jaki typ zawartości ciała przyjmuje metoda; inny Content-Type → 415.
     * Ten sam sku drugi raz → 409 Conflict (konflikt ze stanem serwera).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> create(@RequestBody Product product) {
        if (products.containsKey(product.sku())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produkt " + product.sku() + " już istnieje");
        }
        save(product);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()   // http://localhost:8080/api/products
                .path("/{sku}")                                            // + /{sku}
                .buildAndExpand(product.sku())                             // {sku} → ODZ-002
                .toUri();
        return ResponseEntity.created(location).body(product);
    }

    /**
     * POST /api/products/quick — to samo co create, ale krócej: {@code @ResponseStatus(HttpStatus.CREATED)} NA METODZIE
     * ustawia status 201 bez ResponseEntity. Wada: nie dodasz tak nagłówka Location.
     */
    @PostMapping(value = "/quick", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Product quickCreate(@RequestBody Product product) {
        return save(product);
    }

    // ---------------------------------------------------------------- 3. 204 No Content

    /** DELETE /api/products/KSI-001 → 204 No Content (usunięto, nie ma nic do pokazania); brak → 404. */
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku) {
        return products.remove(sku) != null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ---------------------------------------------------------------- 4. trzy sposoby na 404

    /** GET /api/products/ELE-001/details → 200; brak → ResponseStatusException z kodem 404 i powodem. */
    @GetMapping("/{sku}/details")
    public Product details(@PathVariable String sku) {
        return find(sku).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu " + sku));
    }

    /** GET /api/products/ELE-001/stock → 200 i liczba; brak → własny wyjątek z {@code @ResponseStatus(NOT_FOUND)}. */
    @GetMapping("/{sku}/stock")
    public int stock(@PathVariable String sku) {
        return find(sku).map(Product::stock).orElseThrow(() -> new ProductNotFoundException(sku));
    }

    private Optional<Product> find(String sku) {
        return Optional.ofNullable(products.get(sku));
    }

    private Product save(Product product) {
        products.put(product.sku(), product);
        return product;
    }
}
