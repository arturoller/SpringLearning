package s10_transactions.tx04_read_only_isolation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Tx04ReadOnlyIsolation (bez weba). Profil {@code tx04} wczytuje
 * {@code application-tx04.properties}: własną bazę H2, Flyway z {@code db/migration/tx04}, {@code ddl-auto=validate}.
 * Testy uruchamiają ją jako PEŁNĄ aplikację BEZ transakcji testu (s10_transactions/Tx01Transactional, sekcja 5).
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Tx04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Tx04Application.class);
        app.setAdditionalProfiles("tx04"); // profile = profil ustawień
        app.run(args).close();
    }
}
