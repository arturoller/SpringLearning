package s05_validation.val01_bean_validation;

import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * SZKIELET ĆWICZEŃ lekcji Val01BeanValidation — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Val01BeanValidationExercisesTest). Dopóki metody rzucają UnsupportedOperationException("TODO"), testy ćwiczeń nie
 * przechodzą (są wyłączone z domyślnego {@code ./mvnw test}). Rozwiązania wzorcowe:
 * {@code s05_validation.solutions.val01_bean_validation.ValidationExercisesSolutions}.
 */
public final class ValidationExercises {

    private ValidationExercises() {
    }

    /** Ćwiczenie 1: true, gdy obiekt nie łamie żadnego ograniczenia. */
    public static boolean isValid(Validator validator, Object object) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: posortowana rosnąco lista ścieżek pól, które łamią ograniczenia (bez powtórzeń). */
    public static List<String> invalidFields(Validator validator, Object object) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: mapa pole → posortowana lista NAZW złamanych ograniczeń (prosta nazwa klasy adnotacji, np. "NotBlank").
     * Klucze też posortowane (użyj TreeMap), żeby wynik był przewidywalny.
     */
    public static Map<String, List<String>> constraintsByField(Validator validator, Object object) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: czy SAMA cena byłaby poprawna w {@link ProductForm}? Bez tworzenia całego formularza. */
    public static boolean isPriceAllowed(Validator validator, BigDecimal price) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 5: raport — posortowane linie w postaci {@code "pole: Nazwa (wartość)"}, np. {@code "quantity: Min (0)"}.
     * Wartość to {@code String.valueOf(invalidValue)}, więc brak danych daje "null".
     */
    public static List<String> report(Validator validator, Object object) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
