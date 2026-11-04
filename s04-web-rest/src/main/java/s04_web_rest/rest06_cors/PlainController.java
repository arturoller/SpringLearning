package s04_web_rest.rest06_cors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sekcja 1 lekcji Rest06Cors: kontroler BEZ żadnej konfiguracji CORS (ścieżka spoza /rest06/api/**). */
@RestController
@RequestMapping("/rest06/plain")
public class PlainController {

    @GetMapping("/hello")
    public String hello() {
        return "cześć";
    }
}
