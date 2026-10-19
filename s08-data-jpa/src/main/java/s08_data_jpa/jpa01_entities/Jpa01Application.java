package s08_data_jpa.jpa01_entities;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Klasa startowa lekcji Jpa01Entities. Włącza profil {@code jpa01} (plik application-jpa01.properties: własna baza
 * H2, Flyway wyłączony, logger SQL). Brak serwera WWW — aplikacja zbuduje kontekst, Hibernate utworzy tabele
 * (w logu zobaczysz polecenia {@code create table ...}), a przy zamykaniu je usunie ({@code drop table ...}).
 * Skanuje tylko pakiet tej lekcji, więc nie widzi encji innych lekcji.
 */
@SpringBootApplication
public class Jpa01Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Jpa01Application.class).profiles("jpa01").run(args);
    }
}
