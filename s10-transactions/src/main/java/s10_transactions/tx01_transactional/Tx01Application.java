package s10_transactions.tx01_transactional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx01Transactional (bez weba). Profil {@code tx01} wczytuje
 * {@code application-tx01.properties}: własną bazę H2, Flyway z {@code db/migration/tx01}, {@code ddl-auto=validate}.
 * Testy uruchamiają ją jako PEŁNĄ aplikację BEZ transakcji testu — tylko tak widać prawdziwy commit i rollback
 * (sekcja 5 lekcji, PUŁAPKA testów).
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Tx01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx01Application.class);
        app.setAdditionalProfiles("tx01"); // profile = profil ustawień
        app.run(args).close();
    }
}
