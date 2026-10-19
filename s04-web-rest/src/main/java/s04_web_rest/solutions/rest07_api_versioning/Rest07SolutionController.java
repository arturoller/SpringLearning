package s04_web_rest.solutions.rest07_api_versioning;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import s04_web_rest.rest07_api_versioning.Rest07ExerciseController.CustomerV1;
import s04_web_rest.rest07_api_versioning.Rest07ExerciseController.CustomerV2;

import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń 1–2 lekcji Rest07ApiVersioning — zajrzyj dopiero po własnej próbie!
 * Rekordy CustomerV1 i CustomerV2 pochodzą ze szkieletu ćwiczeń. Wersjonowanie w testach: profil rest07.
 */
@RestController
@RequestMapping("/api/cw07/customers")
public class Rest07SolutionController {

    /** Dane klientów w jednym miejscu — obie wersje API to tylko dwa różne „widoki” na nie. */
    private record Person(String firstName, String lastName, String city) {
    }

    private static final Map<Long, Person> PEOPLE = Map.of(
            1L, new Person("Jan", "Kowalski", "Warszawa"),
            4L, new Person("Zofia", "Krawczyk", "Warszawa"));

    /** Rozwiązanie 1: wersja bazowa "1.0+" obsługuje 1.0, 1.1, 1.5... aż do 2.0. */
    @GetMapping(path = "/{id}", version = "1.0+")
    public CustomerV1 oneV1(@PathVariable long id) {
        Person person = find(id);
        return new CustomerV1(id, person.firstName() + " " + person.lastName());
    }

    /** Rozwiązanie 2: ten sam adres, wersja bazowa "2.0+" — przejmuje adres od 2.0 wzwyż. */
    @GetMapping(path = "/{id}", version = "2.0+")
    public CustomerV2 oneV2(@PathVariable long id) {
        Person person = find(id);
        return new CustomerV2(id, person.firstName(), person.lastName(), person.city());
    }

    private static Person find(long id) {
        Person person = PEOPLE.get(id);
        if (person == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return person;
    }
}
