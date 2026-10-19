package s21_capstone.cap01_shop_api.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s21_capstone.cap01_shop_api.common.InvalidRequestException;
import s21_capstone.cap01_shop_api.common.PageResponse;
import s21_capstone.cap01_shop_api.product.ProductDtos.CreateProductRequest;
import s21_capstone.cap01_shop_api.product.ProductDtos.ProductResponse;
import s21_capstone.cap01_shop_api.product.ProductDtos.UpdateProductRequest;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

/**
 * Katalog produktów. Kontroler zna tylko HTTP: adresy, parametry, statusy i DTO. Reguły są w {@link ProductService}.
 *
 * <pre>
 *   GET    /api/products        200 strona wyników / 400 zły parametr
 *          parametry (wszystkie opcjonalne): category, q (fragment nazwy), page (od 0), size (1–50), sort (biała lista)
 *          np. {@code /api/products?category=KSIAZKI&q=kod&page=0&size=5&sort=price}
 *   GET    /api/products/{sku}  200 / 404
 *   POST   /api/products        201 + Location / 400 walidacja / 409 sku zajęty
 *   PUT    /api/products/{sku}  200 / 400 / 404
 *   DELETE /api/products/{sku}  204 / 404 / 409 produkt jest w zamówieniach
 * </pre>
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /**
     * BIAŁA LISTA sortowania (whitelist = lista dozwolonych wartości): klient wybiera klucz, a my decydujemy, po
     * jakich polach encji to oznacza. Każde sortowanie kończy się unikalnym {@code sku} — przy równych cenach kolejność
     * stron jest wtedy stała (s08_data_jpa/Jpa05PagingSorting, sekcja 4).
     */
    static final Map<String, Sort> SORTS = new TreeMap<>(Map.of(
            "name", Sort.by("name", "sku"),
            "price", Sort.by("price", "sku"),
            "price,desc", Sort.by(Sort.Order.desc("price"), Sort.Order.asc("sku")),
            "sku", Sort.by("sku")));

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * {@code @Min}/{@code @Max} na parametrach działają bez {@code @Validated} na klasie: od Spring 6.1 kontroler sam
     * uruchamia walidację metody (method validation), a błąd to 400 z ProblemDetail.
     */
    @GetMapping
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "strony liczymy od 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "rozmiar strony: od 1 do 50")
            @Max(value = 50, message = "rozmiar strony: od 1 do 50") int size,
            @RequestParam(defaultValue = "name") String sort) {
        Sort chosen = SORTS.get(sort);
        if (chosen == null) {
            throw new InvalidRequestException("SORT_NOT_ALLOWED", "Nie można sortować po: " + sort,
                    Map.of("allowed", SORTS.keySet()));
        }
        return productService.search(category, q, PageRequest.of(page, size, chosen));
    }

    @GetMapping("/{sku}")
    public ProductResponse one(@PathVariable String sku) {
        return productService.get(sku);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{sku}").buildAndExpand(created.sku()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{sku}")
    public ProductResponse update(@PathVariable String sku, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(sku, request);
    }

    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> delete(@PathVariable String sku) {
        productService.delete(sku);
        return ResponseEntity.noContent().build();
    }
}
