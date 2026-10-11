package s11_testing.test01_test_slices;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * ProductController = kontroler produktów: {@code GET /api/products/{id}} i {@code GET /api/products?q=kawa}.
 * Nie zna repozytorium — rozmawia tylko z serwisem. Dzięki temu w {@code @WebMvcTest} wystarczy atrapa serwisu.
 */
@RestController
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/api/products/{id}")
    public ProductDto byId(@PathVariable long id) {
        return service.byId(id);
    }

    @GetMapping("/api/products")
    public List<ProductDto> search(@RequestParam(name = "q", defaultValue = "") String fragment) {
        return service.search(fragment);
    }

    /** Brak produktu → 404 Not Found (nie znaleziono) z krótkim komunikatem. Pełna obsługa błędów: dział s06_error_handling. */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(NoSuchElementException e) {
        return Map.of("error", e.getMessage());
    }
}
