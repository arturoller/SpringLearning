package s04_web_rest.rest06_cors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sekcja 3 lekcji Rest06Cors: CORS ustawiony adnotacją {@code @CrossOrigin} (cross origin = między pochodzeniami)
 * na klasie i na jednej metodzie. Ustawienia z metody są ŁĄCZONE z ustawieniami z klasy.
 */
@RestController
@RequestMapping("/rest06/partner")
@CrossOrigin(origins = "https://partner.example.com", maxAge = 600)
public class PartnerController {

    /** Tylko pochodzenie z klasy: partner.example.com. */
    @GetMapping("/offers")
    public String offers() {
        return "oferty dla partnera";
    }

    /** Pochodzenie z klasy + pochodzenie z metody: partner.example.com ORAZ blog.example.com. */
    @GetMapping("/news")
    @CrossOrigin(origins = "https://blog.example.com")
    public String news() {
        return "nowości";
    }
}
