package s06_error_handling.err04_problem_detail;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Kontroler lekcji Err04ProblemDetail: ZERO handlerów błędów. Każdy adres rzuca inny rodzaj wyjątku:
 * domenowy (własny), {@code ResponseStatusException}, {@code ErrorResponseException}, wyjątek walidacji albo nieoczekiwany.
 */
@RestController
@RequestMapping("/err04/products")
public class ShopController {

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

    /** Wyjątek Springa z gotowym statusem i krótkim opisem (znany z s04_web_rest/Rest03Responses). */
    @GetMapping("/{id}/reserve")
    public String reserve(@PathVariable long id) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Produkt " + id + " jest już zarezerwowany");
    }

    /** Wyjątek, który sam niesie ProblemDetail (ErrorResponseException). */
    @GetMapping("/{id}/archive")
    public String archive(@PathVariable long id) {
        throw new ProductArchivedException(id);
    }

    @GetMapping("/crash")
    public String crash() {
        throw new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'");
    }
}
