package s07_data_jdbc.jdbc05_repository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc05Repository (bez weba). Profil {@code jdbc05} wczytuje
 * {@code application-jdbc05.properties} — bazę, Flyway i lokalizację migracji tej lekcji ({@code db/migration/jdbc05}).
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jdbc05Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jdbc05Application.class);
        app.setAdditionalProfiles("jdbc05"); // profile = profil ustawień
        app.run(args).close();
    }
}
