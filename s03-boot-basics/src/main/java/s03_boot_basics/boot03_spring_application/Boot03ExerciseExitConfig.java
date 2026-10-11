package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Bean;

/**
 * ĆWICZENIE 3 (trudniejsze): bean ExitCodeGenerator, który zwraca kod 2, gdy wśród argumentów NIE-opcji jest słowo
 * „blad” (np. {@code run("blad")}), a w przeciwnym razie 0. Test uruchamia Boot03Application razem z tą klasą
 * i sprawdza wynik SpringApplication.exit(context).
 * Podpowiedź: Boot rejestruje ApplicationArguments jako bean — przyjmij go jako parametr metody {@code @Bean};
 * ExitCodeGenerator ma jedną metodę, więc wystarczy lambda {@code () -> ...}; args.getNonOptionArgs().contains(...).
 * Klasa bez {@code @Configuration} — test dokłada ją sam (Boot03SpringApplicationExercisesTest).
 */
public class Boot03ExerciseExitConfig {

    @Bean
    ExitCodeGenerator errorWordExitCode(ApplicationArguments args) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
