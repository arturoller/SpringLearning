package s04_web_rest.rest01_controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Najprostszy kontroler REST (sekcje 1 i 3 lekcji). {@code @RequestMapping("/rest01")} na klasie to wspólny prefiks
 * (przedrostek) ścieżek wszystkich metod; {@code @GetMapping("/hello")} dokłada resztę → pełna ścieżka {@code /rest01/hello}.
 */
@RestController
@RequestMapping("/rest01")
public class HelloController {

    private final String shopName;

    // @Value czyta ustawienie rest01.shop-name; po dwukropku jest wartość domyślna (gdy ustawienia nie ma).
    public HelloController(@Value("${rest01.shop-name:Sklep DP21}") String shopName) {
        this.shopName = shopName;
    }

    /** String → konwerter napisów → odpowiedź {@code text/plain}. */
    @GetMapping("/hello")
    public String hello() {
        return "Witaj w sklepie: " + shopName + "!";
    }

    /** Obiekt (tu rekord) → konwerter JSON (Jackson) → odpowiedź {@code application/json}. */
    @GetMapping("/hello-json")
    public Greeting helloJson() {
        return new Greeting("Witaj w sklepie: " + shopName + "!", 1);
    }

    /** Rekord (record = zwięzła klasa na dane, niezmienna) — Jackson 3 zamienia go na {@code {"message":…,"version":…}}. */
    public record Greeting(String message, int version) {
    }
}
