package s03_boot_basics.solutions.boot03_spring_application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import s03_boot_basics.boot03_spring_application.Boot03Application;

import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Boot03SpringApplication — zajrzyj dopiero po własnej próbie! */
public final class Boot03SpringApplicationSolutions {

    private static final String BOOT_EVENTS_PACKAGE = "org.springframework.boot.context.event";

    private Boot03SpringApplicationSolutions() {
    }

    /** Rozwiązanie 1: konfigurujemy, ale NIE wołamy run() — to zrobi test. */
    public static SpringApplication solution1Application(String shopMode) {
        SpringApplication application = new SpringApplication(Boot03Application.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(Map.of("shop.mode", shopMode));
        application.setAdditionalProfiles("demo");
        return application;
    }

    /** Rozwiązanie 2: opcje posortowane, wartości po przecinku, potem separator i zwykłe argumenty. */
    public static String solution2DescribeArguments(ApplicationArguments arguments) {
        String options = arguments.getOptionNames().stream()
                .sorted()
                .map(name -> name + "=" + String.join(",", arguments.getOptionValues(name)))
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        return options + " | " + String.join(" ", arguments.getNonOptionArgs());
    }

    /** Rozwiązanie 3: filtr po pakiecie klasy zdarzenia — ContextRefreshedEvent (pakiet Springa) odpada. */
    public static ApplicationListener<ApplicationEvent> solution3BootEventRecorder(List<String> sink) {
        return event -> {
            if (event.getClass().getPackageName().equals(BOOT_EVENTS_PACKAGE)) {
                sink.add(event.getClass().getSimpleName());
            }
        };
    }

    /** Rozwiązanie 4: exit zamyka kontekst i zwraca pierwszy niezerowy kod; zero oznacza „sukces”. */
    public static int solution4ExitCode(ConfigurableApplicationContext context, int failedOrders) {
        return SpringApplication.exit(context, () -> failedOrders == 0 ? 0 : 10 + failedOrders);
    }
}
