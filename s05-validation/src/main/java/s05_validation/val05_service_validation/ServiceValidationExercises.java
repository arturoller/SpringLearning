package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Map;

/**
 * SZKIELET ĆWICZEŃ lekcji Val05ServiceValidation — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Val05ServiceValidationExercisesTest). Dopóki metody rzucają UnsupportedOperationException("TODO"), testy ćwiczeń nie
 * przechodzą (są wyłączone z domyślnego {@code ./mvnw test}). Rozwiązania wzorcowe:
 * {@code s05_validation.solutions.val05_service_validation.ServiceValidationSolutions}.
 */
public final class ServiceValidationExercises {

    private ServiceValidationExercises() {
    }

    /**
     * Ćwiczenie 1: ręczna walidacja obiektu utworzonego przez {@code new}. Gdy są naruszenia, rzuć
     * {@link ConstraintViolationException} (konstruktor przyjmuje zbiór naruszeń); gdy nie ma — zwróć ten sam obiekt.
     */
    public static <T> T validateOrThrow(Validator validator, T object) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 2: opis wyjątku walidacji metody — posortowane linie {@code "ścieżka (NazwaAdnotacji)"},
     * np. {@code "reserve.quantity (Positive)"}.
     */
    public static List<String> describe(ConstraintViolationException exception) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Ćwiczenie 3: czy aplikacja wystartuje z takimi ustawieniami? Uruchom mały kontekst z {@link ShopPropertiesConfig}
     * ({@code new SpringApplicationBuilder(ShopPropertiesConfig.class).web(WebApplicationType.NONE).properties("klucz=wartość").run()})
     * i właściwościami z mapy (klucze pełne, np. {@code "val05.shop.name"}). Zwróć true, gdy kontekst wystartował, false, gdy
     * poleci wyjątek. Pamiętaj o zamknięciu kontekstu (try-with-resources).
     */
    public static boolean startsWith(Map<String, String> properties) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
