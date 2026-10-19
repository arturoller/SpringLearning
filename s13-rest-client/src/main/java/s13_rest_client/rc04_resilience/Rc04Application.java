package s13_rest_client.rc04_resilience;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.resilience.annotation.EnableResilientMethods;

/**
 * Klasa startowa lekcji Rc04Resilience. Włącza profil {@code rc04} (application-rc04.properties).
 *
 * <p>{@code @EnableResilientMethods} (włącz odporne metody, Spring Framework 7) rejestruje dwa post-procesory beanów,
 * które owijają w proxy beany z {@code @Retryable} i {@code @ConcurrencyLimit}. Spring Boot 4.1 NIE włącza tego sam
 * — bez tej adnotacji obie adnotacje są po cichu ignorowane.</p>
 */
@SpringBootApplication
@EnableResilientMethods
public class Rc04Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Rc04Application.class).profiles("rc04").run(args);
    }
}
