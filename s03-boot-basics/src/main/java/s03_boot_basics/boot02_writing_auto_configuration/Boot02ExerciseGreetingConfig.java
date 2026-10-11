package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.context.annotation.Bean;

/**
 * ĆWICZENIE 2 (średnie): konfiguracja użytkownika, która PODMIENIA domyślny GreetingService z autokonfiguracji.
 * Uzupełnij metodę tak, żeby powitanie brzmiało dokładnie: „Dzień dobry, Ola!” (dla klienta „Ola”).
 * Klasa celowo nie ma {@code @Configuration} — test rejestruje ją sam (Boot02WritingAutoConfigurationExercisesTest).
 * Podpowiedź: GreetingService ma jedną metodę, więc wystarczy lambda: {@code customer -> "..." + customer + "!"}.
 */
public class Boot02ExerciseGreetingConfig {

    @Bean
    GreetingService politeGreetingService() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
