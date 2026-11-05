package s07_data_jdbc.jdbc04_flyway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc04Flyway (bez weba). Profil {@code jdbc04} wczytuje {@code application-jdbc04.properties}
 * — bazę w pamięci, WŁĄCZONY Flyway i lokalizację migracji tej lekcji ({@code db/migration/jdbc04}).
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jdbc04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jdbc04Application.class);
        app.setAdditionalProfiles("jdbc04"); // profile = profil ustawień
        app.run(args).close();
    }
}
