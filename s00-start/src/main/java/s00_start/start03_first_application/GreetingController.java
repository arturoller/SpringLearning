package s00_start.start03_first_application;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GreetingController = kontroler powitań. {@code @RestController} (kontroler REST) — metody zwracają dane, które Spring
 * zamienia na JSON i odsyła w odpowiedzi HTTP.
 */
@RestController
public class GreetingController {

    /** Greeting = powitanie. Rekord zamieniany na JSON: {"message": "Cześć, Ala!"}. */
    public record Greeting(String message) {
    }

    private final GreetingService greetingService;

    /**
     * Wstrzykiwanie zależności przez konstruktor (constructor injection): Spring tworzy kontroler i podaje mu bean
     * GreetingService. Jedyny konstruktor nie potrzebuje {@code @Autowired} — Spring użyje go sam.
     */
    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    /**
     * {@code @GetMapping} (mapowanie GET) — ta metoda obsługuje zapytania GET /api/hello.
     * {@code @RequestParam} (parametr zapytania) — wartość z adresu, np. ?name=Ala; bez niego użyjemy wartości domyślnej.
     */
    @GetMapping("/api/hello")
    public Greeting hello(@RequestParam(defaultValue = "świecie") String name) {
        return new Greeting(greetingService.greet(name));
    }
}
