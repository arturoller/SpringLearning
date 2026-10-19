package s08_data_jpa.jpa03_query_methods;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Jpa03QueryMethods (profil {@code jpa03}: własna baza H2, Flyway wyłączony, logger SQL).
 * Już przy starcie Spring Data rozbiera nazwy WSZYSTKICH metod repozytoriów z tego pakietu — literówka w nazwie
 * zatrzyma start aplikacji (sekcja 6 lekcji). Brak serwera WWW — aplikacja zbuduje kontekst i zakończy się.
 */
@SpringBootApplication
public class Jpa03Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Jpa03Application.class).profiles("jpa03").run(args);
    }
}
