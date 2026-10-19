package s04_web_rest.solutions.rest01_controllers;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Rest01Controllers — zajrzyj dopiero po własnej próbie!
 * Osobny pakiet (rodzeństwo lekcji) z własną klasą startową Rest01SolutionsApplication, więc te same adresy
 * /api/cw01/... nie zderzają się ze szkieletem ćwiczeń.
 */
@RestController
@RequestMapping("/api/cw01")
public class Rest01SolutionController {

    /** Rozwiązanie 1: wystarczy zwrócić tekst — @RestController wpisze go do ciała odpowiedzi. */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    /** Rozwiązanie 2: adnotacja mapowania + lista; Jackson zamienia {@code List<String>} na tablicę JSON. */
    @GetMapping("/categories")
    public List<String> categories() {
        return List.of("DOM", "ELEKTRONIKA", "KSIAZKI", "ODZIEZ", "SPOZYWCZE");
    }

    /** Rozwiązanie 3a: GET /api/cw01/cart. */
    @GetMapping("/cart")
    public String cart() {
        return "Koszyk: 0 produktów";
    }

    /** Rozwiązanie 3b: ten sam adres, inna metoda HTTP — to NIE jest duplikat mapowania. */
    @DeleteMapping("/cart")
    public String clearCart() {
        return "Koszyk wyczyszczony";
    }

    /** Rozwiązanie 4: każda para (metoda HTTP, wzorzec) jednego kontrolera jako napis, posortowane. */
    public static List<String> describeEndpoints(RequestMappingHandlerMapping mapping, Class<?> controllerType) {
        return mapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().equals(controllerType))
                .flatMap(entry -> entry.getKey().getMethodsCondition().getMethods().stream()
                        .flatMap(method -> entry.getKey().getPatternValues().stream()
                                .map(pattern -> method.name() + " " + pattern)))
                .sorted()
                .toList();
    }
}
