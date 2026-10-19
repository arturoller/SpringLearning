package s04_web_rest.rest01_controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductController = kontroler produktów: po jednej metodzie na każdą metodę HTTP (GET, POST, PUT, PATCH, DELETE).
 *
 * <p>{@code @RestController} (kontroler REST) = {@code @Controller} + {@code @ResponseBody}: to, co zwraca metoda, trafia
 * prosto do CIAŁA odpowiedzi (jako JSON), a nie jest traktowane jako nazwa widoku (strony HTML).
 * {@code @RequestMapping("/api/products")} na klasie (mapowanie zapytań) — wspólny początek adresu wszystkich metod.</p>
 *
 * <p>Produkty trzymamy w pamięci (ConcurrentHashMap — mapa bezpieczna dla wielu wątków), bo kontroler jest singletonem
 * (jeden obiekt na całą aplikację), a Tomcat obsługuje zapytania równolegle. Prawdziwa baza — od działu s07_data_jdbc.
 * Statusy (201, 204, 404) dopracujemy w lekcji s04_web_rest/Rest03Responses — tu skupiamy się na mapowaniu.</p>
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public ProductController() {
        save(new Product("ELE-001", "Laptop Pro 14", "ELEKTRONIKA", new BigDecimal("5499.99"), 7));
        save(new Product("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", new BigDecimal("64.99"), 120));
        save(new Product("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00"), 15));
    }

    /** GET /api/products → lista produktów posortowana po sku (HashMap nie gwarantuje kolejności — sortujemy sami). */
    @GetMapping
    public List<Product> all() {
        return products.values().stream()
                .sorted(Comparator.comparing(Product::sku))
                .toList();
    }

    /**
     * GET /api/products/ELE-001 → jeden produkt. {@code {sku}} w adresie to zmienna ścieżki ({@code @PathVariable}),
     * szczegóły w s04_web_rest/Rest02RequestData. Brak produktu → wyjątek ResponseStatusException z kodem 404
     * (szczegóły w s04_web_rest/Rest03Responses).
     */
    @GetMapping("/{sku}")
    public Product one(@PathVariable String sku) {
        return find(sku);
    }

    /** POST /api/products z JSON-em w ciele → dodaje produkt i go zwraca (na razie status 200; 201 — w Rest03). */
    @PostMapping
    public Product create(@RequestBody Product product) {
        return save(product);
    }

    /** PUT /api/products/ELE-001 z PEŁNYM produktem w ciele → zastępuje cały produkt (wszystkie pola). */
    @PutMapping("/{sku}")
    public Product replace(@PathVariable String sku, @RequestBody Product product) {
        find(sku);   // najpierw sprawdzamy, czy jest co zastępować (inaczej 404)
        return save(new Product(sku, product.name(), product.category(), product.price(), product.stock()));
    }

    /** PATCH /api/products/ELE-001/stock?delta=-2 → zmienia TYLKO stan magazynu (częściowa zmiana). */
    @PatchMapping("/{sku}/stock")
    public Product changeStock(@PathVariable String sku, @RequestParam int delta) {
        Product current = find(sku);
        return save(current.withStock(current.stock() + delta));
    }

    /**
     * DELETE /api/products/ELE-001 → usuwa produkt. Metoda void (nic nie zwraca) → status 200 i PUSTE ciało.
     * Lepszy jest 204 No Content — pokażemy go w Rest03Responses.
     */
    @DeleteMapping("/{sku}")
    public void delete(@PathVariable String sku) {
        products.remove(sku);
    }

    private Product find(String sku) {
        Product product = products.get(sku);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma produktu " + sku);
        }
        return product;
    }

    private Product save(Product product) {
        products.put(product.sku(), product);
        return product;
    }
}
