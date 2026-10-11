package s03_boot_basics.solutions.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Bean;

/** Rozwiązanie wzorcowe ćwiczenia 3: kod wyjścia 2, gdy wśród zwykłych argumentów jest słowo „blad”. */
public class Boot03ExerciseExitConfigSolution {

    @Bean
    ExitCodeGenerator errorWordExitCode(ApplicationArguments args) {
        return () -> args.getNonOptionArgs().contains("blad") ? 2 : 0;
    }
}
