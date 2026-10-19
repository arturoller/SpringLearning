package s06_error_handling.err03_controller_advice;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler zamówień: {@code @Valid @RequestBody} — błędne dane kończą się MethodArgumentNotValidException. */
@RestController
public class OrderController {

    @PostMapping("/api/orders")
    public String create(@Valid @RequestBody NewOrder order) {
        return "ZAM-001";
    }
}
