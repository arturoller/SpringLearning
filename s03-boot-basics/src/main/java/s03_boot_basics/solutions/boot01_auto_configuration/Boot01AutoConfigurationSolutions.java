package s03_boot_basics.solutions.boot01_auto_configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Boot01AutoConfiguration — zajrzyj dopiero po własnej próbie!
 * Rozwiązanie ćwiczenia 3 (test) jest w Boot01AutoConfigurationSolutionsTest (src/test, ten sam pakiet).
 */
public final class Boot01AutoConfigurationSolutions {

    private Boot01AutoConfigurationSolutions() {
    }

    /** Rozwiązanie 1: domyślne dołączanie pól = tylko niepuste (non_null; wielkość liter bez znaczenia). */
    public static String solution1() {
        return "spring.jackson.default-property-inclusion=non_null";
    }

    /**
     * Rozwiązanie 2a: wyłączenie w adnotacji. Literał klasy — kompilator sprawdzi nazwę (sekcja 6 lekcji).
     * Klasa bez {@code @Configuration}, więc nikt jej nie zeskanuje — uruchamia ją tylko test.
     */
    @EnableAutoConfiguration(exclude = JacksonAutoConfiguration.class)
    public static class Exercise2Application {
    }

    /** Rozwiązanie 2b: wykluczenia z raportu warunków, posortowane. */
    public static List<String> solution2Exclusions(ConfigurableApplicationContext context) {
        return ConditionEvaluationReport.get(context.getBeanFactory()).getExclusions().stream().sorted().toList();
    }
}
