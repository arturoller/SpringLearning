package s08_data_jpa.jpa06_flyway_with_jpa;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Jpa06FlywayWithJpa (profil {@code jpa06}: własna baza H2, migracje z
 * {@code db/migration/jpa06}, ddl-auto=validate, logger SQL). Przy starcie najpierw Flyway wykonuje migracje,
 * potem Hibernate sprawdza schemat. Brak serwera WWW — aplikacja zbuduje kontekst i zakończy się.
 */
@SpringBootApplication
public class Jpa06Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Jpa06Application.class).profiles("jpa06").run(args);
    }
}
