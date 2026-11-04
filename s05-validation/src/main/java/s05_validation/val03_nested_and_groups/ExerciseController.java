package s05_validation.val03_nested_and_groups;

import jakarta.validation.Validator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SZKIELET ĆWICZEŃ lekcji Val03NestedAndGroups — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Val03NestedAndGroupsExercisesTest). Mapowania są gotowe; dodajesz adnotacje walidacyjne i ciała metod. Dopóki metody
 * rzucają UnsupportedOperationException("TODO"), testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/val03/cw/shipments")
public class ExerciseController {

    /** Adres przesyłki. Ćwiczenie 1: street i city mają być niepuste. */
    public record AddressDto(String street, String city) {
    }

    /**
     * Przesyłka. Ćwiczenia 1–3: dodaj ograniczenia.
     * recipient: niepuste; address: wymagany i sprawdzany w głąb (ćw. 1);
     * parcelCodes: lista niepusta, żaden kod nie może być pusty (ćw. 2);
     * id: przy tworzeniu null, przy zmianie wymagane — grupy OnCreate/OnUpdate z tej lekcji (ćw. 3).
     */
    public record ShipmentRequest(Long id, String recipient, AddressDto address, List<String> parcelCodes) {
    }

    /**
     * Ćwiczenia 1–3: odpowiedz 201 i odeślij w ciele otrzymaną przesyłkę (echo). Błędne dane → 400 (automatycznie, po
     * dodaniu odpowiedniej adnotacji przy parametrze). W ćwiczeniu 3 użyj grupy OnCreate.
     */
    @PostMapping
    public ResponseEntity<ShipmentRequest> create(@RequestBody ShipmentRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: odpowiedz 200 i odeślij otrzymaną przesyłkę; id jest wymagane (grupa OnUpdate). */
    @PutMapping
    public ResponseEntity<ShipmentRequest> update(@RequestBody ShipmentRequest request) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 4: posortowana lista UNIKALNYCH ścieżek pól z błędami dla podanych grup (bez grup = Default).
     * Podpowiedź: validator.validate(object, groups) — drugi argument to varargs (zmienna liczba argumentów).
     */
    public static List<String> fieldPaths(Validator validator, Object object, Class<?>... groups) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5: sekwencja grup. Dodaj {@code @GroupSequence({CouponChecks.Basic.class, CouponChecks.Advanced.class})}. */
    public interface Checks {
    }

    /**
     * Ćwiczenie 5: zwróć posortowane napisy {@code "ścieżka:Adnotacja"} (np. {@code "code:NotBlank"}) z walidacji kuponu
     * w kolejności {@link Checks}: gdy grupa Basic ma błędy, Advanced nie jest sprawdzana.
     */
    public static List<String> validateInOrder(Validator validator, CouponForm form) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
