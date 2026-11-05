package s06_error_handling.err05_pitfalls;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s06_error_handling.err05_custom_exceptions.OrderService;
import s06_error_handling.err05_custom_exceptions.ProductNotFoundException;

import java.util.Map;

/** ZŁY kontroler (pułapka z sekcji 5 lekcji Err05CustomExceptions): łapie wyjątek i oddaje 200 z polem "error". */
@RestController
@RequestMapping("/err05/bad")
public class CatchingController {

    private final OrderService service;

    public CatchingController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/products/{id}")
    public Object product(@PathVariable long id) {
        try {
            return service.getProduct(id);
        } catch (ProductNotFoundException exception) {
            return Map.of("error", exception.getMessage());   // status 200! klient widzi "sukces"
        }
    }
}
