package s04_web_rest.solutions.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń 1–3 lekcji Rest07ApiVersioning — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/rest07/cw/customers")
public class Rest07SolutionsController {

    public record CustomerV1(long id, String fullName) {
    }

    public record CustomerV2(long id, String firstName, String lastName) {
    }

    /** Rozwiązanie 1: ciało jak w zwykłym kontrolerze — wersję wybrał już Spring na podstawie mapowania. */
    @GetMapping(path = "/{id}", version = "1.0")
    public CustomerV1 byIdV1(@PathVariable long id) {
        return new CustomerV1(id, "Jan Kowalski");
    }

    /** Rozwiązanie 2: ten sam adres, inna wersja, inny kształt odpowiedzi. */
    @GetMapping(path = "/{id}", version = "2.0")
    public CustomerV2 byIdV2(@PathVariable long id) {
        return new CustomerV2(id, "Jan", "Kowalski");
    }

    /** Rozwiązanie 3: „1.1+” — jedna metoda obsługuje 1.1 i każdą nowszą wersję; dla 1.0 mapowania nie ma (404). */
    @GetMapping(path = "/{id}/orders", version = "1.1+")
    public List<String> orders(@PathVariable long id) {
        return List.of("Z-" + id + "-1", "Z-" + id + "-2");
    }
}
