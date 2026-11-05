package s06_error_handling.err03_controller_advice;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pierwszy kontroler lekcji: ZERO handlerów błędów. Wszystkie błędy obsługuje {@code ShopAdvice} i {@code FallbackAdvice}.
 */
@RestController
@RequestMapping("/err03/products")
public class ProductController {

    @GetMapping("/{id}")
    public Product product(@PathVariable long id) {
        if (id == 1) {
            return new Product(1, "Kawa");
        }
        throw new ProductNotFoundException(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody NewProduct request) {
        return new Product(2, request.name());
    }

    @GetMapping("/{id}/stock")
    public String stock(@PathVariable long id) {
        throw new OutOfStockException("Kawa");
    }

    @GetMapping("/crash")
    public String crash() {
        throw new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'");
    }

    @GetMapping("/priority")
    public String priority() {
        throw new PriorityDemoException();
    }
}
