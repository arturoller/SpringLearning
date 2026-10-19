package s04_web_rest.rest01_controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trzy kontrolery powitania (greeting = powitanie) — porównanie {@code @RestController}, {@code @Controller}
 * + {@code @ResponseBody} i samego {@code @Controller}. Są w jednym pliku, bo to jeden przykład; każda klasa
 * jest osobnym beanem (ziarnem — obiektem zarządzanym przez Springa).
 */
final class GreetingControllers {

    private GreetingControllers() {
    }

    /** {@code @RestController}: zwrócony String trafia do ciała odpowiedzi jako zwykły tekst (text/plain). */
    @RestController
    static class RestGreetingController {

        @GetMapping("/api/greeting/rest")
        String greet() {
            return "Witaj w sklepie";
        }
    }

    /**
     * {@code @Controller} + {@code @ResponseBody} na klasie — DOKŁADNIE to samo co {@code @RestController}
     * (zajrzyj do kodu adnotacji RestController: Ctrl+klik — jest oznaczona {@code @Controller} i {@code @ResponseBody}).
     */
    @Controller
    @ResponseBody   // response body = ciało odpowiedzi: wynik metody idzie do ciała, a nie do widoku
    static class ClassicGreetingController {

        @GetMapping("/api/greeting/classic")
        String greet() {
            return "Witaj w sklepie";
        }
    }

    /**
     * PUŁAPKA: celowo błędny przykład — sam {@code @Controller} BEZ {@code @ResponseBody}. Spring traktuje
     * zwrócony String jako NAZWĘ WIDOKU (view name — nazwa szablonu strony HTML) i próbuje przekazać (forward)
     * zapytanie pod adres „witaj”. Na prawdziwym serwerze klient dostaje 404.
     */
    @Controller
    static class ViewNameController {

        @GetMapping("/api/greeting/view")
        String greet() {
            return "witaj";
        }
    }
}
