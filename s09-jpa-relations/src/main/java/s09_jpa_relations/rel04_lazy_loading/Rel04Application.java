package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rel04LazyLoading (bez weba). Profil {@code rel04} wczytuje
 * {@code application-rel04.properties}: własną bazę H2, Flyway z {@code db/migration/rel04}, {@code ddl-auto=validate}.
 * Testy uruchamiają ją jako PEŁNĄ aplikację (Rel04OutsideTransactionTest) — tylko tak widać prawdziwe granice
 * transakcji i {@code LazyInitializationException}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Rel04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Rel04Application.class);
        app.setAdditionalProfiles("rel04"); // profile = profil ustawień
        app.run(args).close();
    }
}
