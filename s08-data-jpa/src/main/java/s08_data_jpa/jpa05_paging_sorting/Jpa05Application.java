package s08_data_jpa.jpa05_paging_sorting;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Jpa05PagingSorting (profil {@code jpa05}: własna baza H2, Flyway wyłączony, logger SQL).
 * Brak serwera WWW — aplikacja zbuduje kontekst i zakończy się.
 */
@SpringBootApplication
public class Jpa05Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Jpa05Application.class).profiles("jpa05").run(args);
    }
}
