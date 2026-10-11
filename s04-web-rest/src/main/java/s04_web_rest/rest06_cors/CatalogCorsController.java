package s04_web_rest.rest06_cors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CatalogCorsController = publiczny katalog — sekcja 3 lekcji Rest06Cors.
 * {@code @CrossOrigin} (dozwolone inne pochodzenie) na klasie: przeglądarka ze strony https://sklep.example.com może
 * czytać odpowiedzi tego kontrolera. Inne strony — nie.
 */
@RestController
@RequestMapping("/api/catalog")
@CrossOrigin(origins = "https://sklep.example.com")
public class CatalogCorsController {

    @GetMapping
    public List<String> products() {
        return List.of("Kawa", "Herbata", "Czekolada");
    }
}
