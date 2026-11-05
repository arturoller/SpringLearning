package s04_web_rest.solutions.rest06_cors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler do rozwiązania 5 lekcji Rest06Cors — taki sam jak ExerciseController w lekcji. */
@RestController
@RequestMapping("/rest06/cw/orders")
public class Rest06SolutionsController {

    @GetMapping
    public String orders() {
        return "zamówienia";
    }

    @PostMapping
    public String create() {
        return "utworzono";
    }
}
