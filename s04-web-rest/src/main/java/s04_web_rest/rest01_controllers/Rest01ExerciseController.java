package s04_web_rest.rest01_controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * Ćwiczenia lekcji Rest01Controllers — szkielety do uzupełnienia. Testy: Rest01ControllersExercisesTest
 * (uruchamiasz sam: ▶ obok klasy testu). Wszystkie adresy zaczynają się od /api/cw01, żeby nie kolidowały z lekcją.
 */
@RestController
@RequestMapping("/api/cw01")
public class Rest01ExerciseController {

    /**
     * ĆWICZENIE 1 (łatwe): GET /api/cw01/ping ma odpowiadać tekstem "pong" (status 200).
     * Mapowanie już jest — uzupełnij tylko ciało metody.
     */
    @GetMapping("/ping")
    public String ping() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): GET /api/cw01/categories ma zwracać JSON-ową tablicę kategorii sklepu w kolejności
     * alfabetycznej: ["DOM","ELEKTRONIKA","KSIAZKI","ODZIEZ","SPOZYWCZE"].
     * Tu brakuje też ADNOTACJI mapowania — bez niej test dostaje 404. Podpowiedź: {@code @GetMapping("/categories")}
     * i zwracany typ {@code List<String>} — Jackson sam zamieni listę na tablicę JSON.
     */
    public List<String> categories() {
        // TODO: twoje rozwiązanie (dodaj adnotację mapowania nad metodą)
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): JEDEN adres, DWIE metody HTTP. GET /api/cw01/cart → tekst "Koszyk: 0 produktów",
     * DELETE /api/cw01/cart → tekst "Koszyk wyczyszczony". POST /api/cw01/cart ma dawać 405.
     * Podpowiedź: ten sam adres może mieć różne adnotacje metod HTTP — to nie jest duplikat mapowania.
     * Metoda cart() ma już mapowanie, clearCart() — jeszcze nie.
     */
    @GetMapping("/cart")
    public String cart() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Część ĆWICZENIA 3: dodaj adnotację DELETE dla adresu /cart i uzupełnij ciało. */
    public String clearCart() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): „spis pokoi recepcji”. Zwróć listę punktów końcowych (endpointów) podanego kontrolera
     * w postaci "METODA wzorzec", posortowaną alfabetycznie, np. dla ProductController:
     * ["DELETE /api/products/{sku}", "GET /api/products", "GET /api/products/{sku}", "PATCH /api/products/{sku}/stock",
     * "POST /api/products", "PUT /api/products/{sku}"].
     * Podpowiedź: {@code mapping.getHandlerMethods()} to mapa RequestMappingInfo → HandlerMethod (pakiety:
     * org.springframework.web.servlet.mvc.method i org.springframework.web.method);
     * {@code handlerMethod.getBeanType()} — klasa kontrolera; {@code info.getMethodsCondition().getMethods()} — metody
     * HTTP (RequestMethod); {@code info.getPatternValues()} — wzorce adresów. Dla każdej pary (metoda HTTP, wzorzec)
     * jeden napis. Strumienie: t16_streams/Streams04FlatMap.
     */
    public static List<String> describeEndpoints(RequestMappingHandlerMapping mapping, Class<?> controllerType) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
