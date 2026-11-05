package s04_web_rest.rest02_request_data;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sekcje 4 i 5 lekcji: ciało zapytania ({@code @RequestBody}, JSON → rekord) kontra dane formularza ({@code @RequestParam}). */
@RestController
@RequestMapping("/rest02/reviews")
public class ReviewController {

    /**
     * Rekord z JSON-a. Pola referencyjne (String, Integer) przy braku w JSON-ie dostają null. Pole prymitywne (int)
     * nie może być null — patrz {@link Score}.
     */
    public record ReviewRequest(String author, Integer rating, String text) {
    }

    /** Rekord z polem prymitywnym — brak pola w JSON-ie to w Jackson 3 błąd 400 (w Jackson 2 byłoby po cichu 0). */
    public record Score(int points) {
    }

    /** JSON w ciele zapytania → rekord. Zły JSON albo brak ciała → 400. Zwracamy to, co Spring odczytał. */
    @PostMapping("/json")
    public ReviewRequest fromJson(@RequestBody ReviewRequest review) {
        return review;
    }

    @PostMapping("/score")
    public Score score(@RequestBody Score score) {
        return score;
    }

    /** Dane formularza (Content-Type: application/x-www-form-urlencoded) czytamy przez @RequestParam, NIE przez @RequestBody. */
    @PostMapping("/form")
    public String fromForm(@RequestParam String author, @RequestParam int rating) {
        return "formularz: " + author + " " + rating;
    }
}
