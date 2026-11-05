package s04_web_rest.rest01_controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Sekcja 1 lekcji: zwykły {@code @Controller} (kontroler widoków). Zwrócony String to NAZWA WIDOKU (view), nie treść
 * odpowiedzi — chyba że metoda (albo klasa) ma {@code @ResponseBody} (= „zwrócona wartość JEST ciałem odpowiedzi”).
 * {@code @RestController} to dokładnie {@code @Controller + @ResponseBody} na całej klasie.
 */
@Controller
public class ClassicController {

    @GetMapping("/rest01/classic-body")
    @ResponseBody
    public String body() {
        return "to jest treść odpowiedzi";
    }

    @GetMapping("/rest01/classic-view")
    public String view() {
        return "nazwa-widoku"; // BEZ @ResponseBody: Spring szuka widoku o tej nazwie, a nie wysyła tego napisu
    }
}
