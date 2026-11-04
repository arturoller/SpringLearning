package s06_error_handling.err03_controller_advice.legacy;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler w pakiecie {@code legacy} — obsługuje go {@code LegacyAdvice} (selektor {@code basePackages}). */
@RestController
@RequestMapping("/err03/legacy")
public class LegacyController {

    @GetMapping("/export")
    public String export() {
        throw new IllegalArgumentException("Zły format eksportu");
    }
}
