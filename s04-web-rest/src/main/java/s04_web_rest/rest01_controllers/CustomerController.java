package s04_web_rest.rest01_controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CustomerController = kontroler klientów — SZKIELET ćwiczeń 1 i 2 z lekcji Rest01Controllers.
 * Test: Rest01ControllersExercisesTest (@WebMvcTest). Rozwiązanie: pakiet s04_web_rest.solutions.rest01_controllers.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    /** Klienci sklepu — dane do ćwiczeń (lista niezmienna). */
    static final List<String> CUSTOMERS = List.of("Anna", "Bartek", "Celina");

    /**
     * ĆWICZENIE 1 (łatwe): {@code GET /api/customers} ma zwrócić 200 OK i JSON-ową tablicę imion
     * {@code ["Anna","Bartek","Celina"]}. Adres jest gotowy (adnotacje na klasie i metodzie) — uzupełnij ciało.
     * Podpowiedź: wystarczy zwrócić listę CUSTOMERS; Jackson sam zamieni {@code List<String>} na tablicę JSON.
     */
    @GetMapping
    public List<String> all() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): {@code GET /api/customers/count} ma zwrócić 200 OK i liczbę klientów (JSON: {@code 3}),
     * a {@code DELETE /api/customers/count} — 405 Method Not Allowed. Ta metoda NIE MA jeszcze adnotacji mapującej:
     * dopisz właściwą (pamiętaj, że początek adresu daje {@code @RequestMapping} na klasie) i uzupełnij ciało.
     * Podpowiedź: bez adnotacji DispatcherServlet w ogóle nie zna tej metody — test dostanie 404.
     */
    public int count() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
