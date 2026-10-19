package s08_data_jpa.jpa04_custom_queries;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Jpa04CustomQueries (profil {@code jpa04}: własna baza H2, Flyway wyłączony, logger SQL).
 * Przy starcie Spring Data sprawdza składnię i nazwy w każdym zapytaniu JPQL z {@code @Query}
 * (zapytań natywnych nie sprawdza — sekcja 2 lekcji). Brak serwera WWW — aplikacja zbuduje kontekst i zakończy się.
 */
@SpringBootApplication
public class Jpa04Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Jpa04Application.class).profiles("jpa04").run(args);
    }
}
