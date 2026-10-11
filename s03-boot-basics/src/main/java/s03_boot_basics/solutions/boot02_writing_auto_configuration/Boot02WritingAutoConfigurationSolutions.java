package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.springframework.context.ApplicationContext;
import s03_boot_basics.boot02_writing_auto_configuration.GreetingService;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Boot02WritingAutoConfiguration — zajrzyj dopiero po własnej próbie!
 * Ćwiczenie 2: Boot02ExerciseGreetingConfigSolution; ćwiczenie 3: Boot02LoyaltyAutoConfigurationSolution.
 */
public final class Boot02WritingAutoConfigurationSolutions {

    private Boot02WritingAutoConfigurationSolutions() {
    }

    static String solution1(ApplicationContext context) {
        return context.getBean(GreetingService.class).greet("Ania");
    }
}
