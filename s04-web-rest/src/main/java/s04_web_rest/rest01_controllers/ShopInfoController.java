package s04_web_rest.rest01_controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * ShopInfoController = kontroler informacji o sklepie — sekcja 2 lekcji Rest01Controllers.
 * Zwykły {@code @Controller} (kontroler) — bez „Rest”. Taki kontroler w Spring MVC domyślnie zwraca NAZWĘ WIDOKU
 * (view — szablon strony HTML), a nie dane. Żeby zwrócić dane, metoda potrzebuje {@code @ResponseBody}
 * (ciało odpowiedzi).
 */
@Controller
@RequestMapping("/api/shop")
public class ShopInfoController {

    /** GET /api/shop/info — {@code @ResponseBody}: rekord trafia do ciała odpowiedzi jako JSON. Jak w @RestController. */
    @GetMapping("/info")
    @ResponseBody
    public Reply info() {
        return new Reply("ShopInfoController.info", "sklep otwarty 8–20");
    }

    /**
     * GET /api/shop/forgotten — ZŁY PRZYKŁAD: brak {@code @ResponseBody}. String „shop-hours” NIE trafia do ciała
     * odpowiedzi — Spring traktuje go jak nazwę widoku i próbuje przekazać (forward) zapytanie pod adres „shop-hours”.
     * Test pokazuje, co z tego wychodzi (Rest01ControllersTest, sekcja 2).
     */
    @GetMapping("/forgotten")
    public String forgottenResponseBody() {
        return "shop-hours";
    }
}
