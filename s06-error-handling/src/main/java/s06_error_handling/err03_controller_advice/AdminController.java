package s06_error_handling.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler panelu administratora — obsługuje go {@code AdminAdvice} (selektor {@code assignableTypes}). */
@RestController
@RequestMapping("/err03/admin")
public class AdminController {

    @GetMapping("/discount")
    public String discount() {
        throw new IllegalArgumentException("Rabat musi być w przedziale 0-100");
    }
}
