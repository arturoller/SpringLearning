package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.springframework.context.annotation.Bean;
import s03_boot_basics.boot02_writing_auto_configuration.GreetingService;

/** Rozwiązanie wzorcowe ćwiczenia 2: własny GreetingService — domyślny z autokonfiguracji się wycofa. */
public class Boot02ExerciseGreetingConfigSolution {

    @Bean
    GreetingService politeGreetingService() {
        return customer -> "Dzień dobry, " + customer + "!";
    }
}
