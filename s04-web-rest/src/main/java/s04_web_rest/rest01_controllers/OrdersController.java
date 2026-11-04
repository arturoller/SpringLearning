package s04_web_rest.rest01_controllers;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sekcja 2 lekcji: ta sama ścieżka {@code /rest01/orders}, ale pięć różnych metod HTTP → pięć różnych metod Javy.
 * Zwracamy tylko opisy tekstowe — chodzi o samo mapowanie, nie o logikę zamówień.
 */
@RestController
@RequestMapping("/rest01/orders")
public class OrdersController {

    @GetMapping
    public String list() {
        return "GET: lista zamówień";
    }

    @PostMapping
    public String create() {
        return "POST: nowe zamówienie";
    }

    @PutMapping
    public String replace() {
        return "PUT: podmiana zamówienia w całości";
    }

    @PatchMapping
    public String change() {
        return "PATCH: zmiana części zamówienia";
    }

    @DeleteMapping
    public String delete() {
        return "DELETE: usunięcie zamówienia";
    }

    // Klasyczny zapis, którego nauczysz się rozpoznawać w starszym kodzie:
    //   @RequestMapping(path = "/legacy", method = RequestMethod.GET)  ≡  @GetMapping("/legacy")
    // Skrótowe adnotacje są po prostu krótszą formą @RequestMapping z ustaloną metodą HTTP.
    @RequestMapping(path = "/legacy", method = RequestMethod.GET)
    public String legacy() {
        return "GET (zapis klasyczny): to samo co @GetMapping";
    }
}
