package s06_error_handling.err03_controller_advice.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Kontroler panelu administratora — w osobnym podpakiecie, żeby pokazać zawężanie porady po pakiecie ({@code basePackages}). */
@RestController
public class AdminController {

    @GetMapping("/api/admin/report")
    public String report() {
        throw new IllegalStateException("Generator raportów jest wyłączony");
    }
}
