package s05_validation.val05_service_validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SZKIELET ćwiczenia 3 z lekcji Val05ServiceValidation (opis: Val05ServiceValidation): ustawienia serwera poczty
 * z kluczami {@code val05.exercise.*}. Test: Val05ServiceValidationExercisesTest (ApplicationContextRunner).
 */
@ConfigurationProperties("val05.exercise")
// TODO: ćwiczenie 3 — dopisz adnotację włączającą walidację i reguły przy komponentach
public record ExerciseProperties(String host, int port, Admin admin) {

    /** Admin = dane administratora (zagnieżdżone). */
    public record Admin(String email) {
    }
}
