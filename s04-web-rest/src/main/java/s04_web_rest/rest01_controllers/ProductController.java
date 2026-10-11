package s04_web_rest.rest01_controllers;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ProductController = kontroler produktów — sekcje 1 i 3 lekcji Rest01Controllers.
 * {@code @RestController} (kontroler REST) = {@code @Controller} + {@code @ResponseBody}: wynik każdej metody trafia
 * do CIAŁA odpowiedzi (jako JSON), a nie jest traktowany jak nazwa widoku (strony HTML).
 * {@code @RequestMapping("/api/products")} na klasie = wspólny początek adresu wszystkich metod.
 * Metody tylko mówią, która z nich zadziałała — prawdziwe dane i kody odpowiedzi są w Rest02–Rest05.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    /** GET /api/products — odczyt listy. Pusta {@code @GetMapping} = sam adres z klasy. */
    @GetMapping
    public List<Reply> all() {
        return List.of(new Reply("ProductController.all", "lista produktów"));
    }

    /** GET /api/products/7 — odczyt jednego produktu. {@code {id}} = zmienna w ścieżce (szczegóły w Rest02). */
    @GetMapping("/{id}")
    public Reply one(@PathVariable long id) {
        return new Reply("ProductController.one", "produkt " + id);
    }

    /** POST /api/products — utworzenie nowego produktu (post = wyślij, dodaj). */
    @PostMapping
    public Reply create() {
        return new Reply("ProductController.create", "utworzono produkt");
    }

    /** PUT /api/products/7 — zastąpienie CAŁEGO produktu (put = połóż, wstaw w miejsce). */
    @PutMapping("/{id}")
    public Reply replace(@PathVariable long id) {
        return new Reply("ProductController.replace", "zastąpiono produkt " + id);
    }

    /** PATCH /api/products/7 — zmiana CZĘŚCI produktu (patch = łata). */
    @PatchMapping("/{id}")
    public Reply patch(@PathVariable long id) {
        return new Reply("ProductController.patch", "poprawiono produkt " + id);
    }

    /** DELETE /api/products/7 — usunięcie produktu (delete = usuń). */
    @DeleteMapping("/{id}")
    public Reply delete(@PathVariable long id) {
        return new Reply("ProductController.delete", "usunięto produkt " + id);
    }

    /**
     * GET /api/products/bestsellers — stała ścieżka obok wzorca /{id}. Spring wybiera BARDZIEJ KONKRETNE dopasowanie
     * (sama stała ścieżka wygrywa ze zmienną), więc „bestsellers” nie trafia do one(...) jako id.
     */
    @GetMapping("/bestsellers")
    public Reply bestsellers() {
        return new Reply("ProductController.bestsellers", "najlepiej sprzedające się produkty");
    }
}
