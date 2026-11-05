package s04_web_rest.solutions.rest01_controllers;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest01Controllers — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/rest01/cw")
public class Rest01SolutionsController {

    public record ShopInfo(String name, int openDays) {
    }

    /** Rozwiązanie 1: zwykły String → text/plain. */
    @GetMapping("/greeting")
    public String greeting() {
        return "Witaj w sklepie!";
    }

    /** Rozwiązanie 2: rekord → konwerter JSON → application/json. */
    @GetMapping("/shop")
    public ShopInfo shop() {
        return new ShopInfo("Sklep DP21", 6);
    }

    /** Rozwiązanie 3: ta sama ścieżka, dwie metody HTTP → dwie metody Javy. */
    @GetMapping("/cart")
    public String cart() {
        return "koszyk";
    }

    @DeleteMapping("/cart")
    public String emptyCart() {
        return "koszyk opróżniony";
    }

    /** Rozwiązanie 4: ograniczenie przez produces — Spring sam odpowie 406 na inne Accept. */
    @GetMapping(path = "/report", produces = "text/csv")
    public String report() {
        return "id;name";
    }
}
