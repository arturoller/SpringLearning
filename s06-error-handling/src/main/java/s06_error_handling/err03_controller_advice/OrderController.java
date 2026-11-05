package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Drugi kontroler lekcji — też bez handlerów; korzysta z TEGO SAMEGO advice co {@link ProductController}. */
@RestController
@RequestMapping("/err03/orders")
public class OrderController {

    @GetMapping("/{id}")
    public String order(@PathVariable long id) {
        if (id == 1) {
            return "Zamówienie #1";
        }
        throw new OrderNotFoundException(id);
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable long id) {
        throw new OutOfStockException("Herbata");
    }
}
