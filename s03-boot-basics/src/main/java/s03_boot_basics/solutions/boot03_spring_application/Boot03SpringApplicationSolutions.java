package s03_boot_basics.solutions.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import s03_boot_basics.boot03_spring_application.Boot03Application;

import java.util.Map;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Boot03SpringApplication — zajrzyj dopiero po własnej próbie!
 * Ćwiczenie 3: Boot03ExerciseExitConfigSolution.
 */
public final class Boot03SpringApplicationSolutions {

    private Boot03SpringApplicationSolutions() {
    }

    static String solution1(ApplicationArguments args) {
        return args.containsOption("import.file") ? args.getOptionValues("import.file").getFirst() : "brak";
    }

    static String solution2(String... args) {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setDefaultProperties(Map.of("boot03.shop-name", "Sklep Ćwiczeniowy"));
        try (ConfigurableApplicationContext context = application.run(args)) {
            return context.getEnvironment().getProperty("boot03.shop-name");
        }
    }
}
