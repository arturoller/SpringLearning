package s05_validation.val02_valid_in_controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SZKIELET ĆWICZEŃ lekcji Val02ValidInControllers — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Val02ValidInControllersExercisesTest). Mapowania są gotowe; dodajesz adnotacje walidacyjne i ciała metod. Dopóki metody
 * rzucają UnsupportedOperationException("TODO"), testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/val02/cw/customers")
public class ExerciseController {

    /**
     * Dane wejściowe klienta. Ćwiczenie 1: dodaj ograniczenia — name: niepuste; email: niepusty i w kształcie adresu
     * e-mail (import: jakarta.validation.constraints.*).
     */
    public record CustomerRequest(String name, String email) {
    }

    private final Customers customers;

    public ExerciseController(Customers customers) {
        this.customers = customers;
    }

    /**
     * Ćwiczenie 1: utwórz klienta (customers.add) i odpowiedz 201 z klientem w ciele. Błędne dane (po dodaniu
     * ograniczeń do CustomerRequest) mają dać 400 i NIE dodać klienta. Podpowiedź: {@code @Valid} przy parametrze.
     */
    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody CustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 2: 200 z klientem albo 404, gdy go nie ma. Id mniejsze niż 1 ma dać 400 (zanim ruszy serwis).
     * Podpowiedź: ograniczenie przy parametrze {@code @PathVariable}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> get(@PathVariable long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: jak create, ale BEZ automatycznego 400. Dodaj {@code BindingResult} zaraz po parametrze z
     * {@code @Valid}. Gdy są błędy — 400 z listą tekstów {@code "pole:Kod"} posortowaną alfabetycznie (np.
     * {@code ["email:NotBlank", "name:NotBlank"]}); inaczej 201 z nowym klientem.
     */
    @PostMapping("/manual")
    public ResponseEntity<?> createManual(@RequestBody CustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 4: zamień błędy z {@link BindingResult} na mapę pole → komunikat, posortowaną po nazwie pola (TreeMap).
     * Gdy pole ma kilka błędów — połącz komunikaty (posortowane alfabetycznie) tekstem {@code "; "}.
     * To jest ciało, które w prawdziwym projekcie trafi do @ExceptionHandler (s06_error_handling/Err02ExceptionHandler).
     */
    public static Map<String, String> messagesByField(BindingResult bindingResult) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 5: zmień klienta (customers.update); 200 z klientem albo 404. Id mniejsze niż 1 → 400, błędne ciało
     * → 400; w obu przypadkach serwis nie może zostać wywołany. Podpowiedź: łączysz ograniczenie przy {@code id}
     * i {@code @Valid} przy ciele.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Customer> replace(@PathVariable long id, @RequestBody CustomerRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
