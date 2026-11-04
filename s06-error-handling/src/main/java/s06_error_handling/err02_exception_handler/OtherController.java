package s06_error_handling.err02_exception_handler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Drugi kontroler lekcji — NIE ma żadnych handlerów. Rzuca ten sam wyjątek {@code ProductNotFoundException}, który
 * {@link ShopController} umie obsłużyć, ale handler z innego kontrolera tu nie sięga (sekcja 5).
 */
@RestController
@RequestMapping("/err02/other")
public class OtherController {

    @GetMapping("/products/{id}")
    public Product product(@PathVariable long id) {
        throw new ProductNotFoundException(id);
    }
}
