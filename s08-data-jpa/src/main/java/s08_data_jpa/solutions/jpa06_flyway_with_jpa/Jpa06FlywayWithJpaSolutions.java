package s08_data_jpa.solutions.jpa06_flyway_with_jpa;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import s08_data_jpa.jpa06_flyway_with_jpa.Jpa06Application;

/**
 * Rozwiązania wzorcowe lekcji Jpa06FlywayWithJpa. Ćwiczenie 1 — plik
 * src/main/resources/db/migration/jpa06-solutions/V4__add_weight_column.sql; ćwiczenie 2 — klasa Customer obok.
 */
public final class Jpa06FlywayWithJpaSolutions {

    private Jpa06FlywayWithJpaSolutions() {
    }

    /**
     * Rozwiązanie 3: start w try-with-resources (kontekst zamknie się sam), a przy błędzie —
     * getMostSpecificCause, czyli najgłębsza przyczyna w łańcuchu getCause().
     */
    public static String solution3(String... args) {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(Jpa06Application.class)
                .web(WebApplicationType.NONE).run(args)) {
            return null;
        } catch (RuntimeException startupFailure) {
            return NestedExceptionUtils.getMostSpecificCause(startupFailure).getMessage();
        }
    }
}
