package s07_data_jdbc.jdbc02_jdbc_template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc02JdbcTemplate (bez weba). Profil {@code jdbc02} wczytuje
 * {@code application-jdbc02.properties} — bazę, schemat i dane tej lekcji.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jdbc02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jdbc02Application.class);
        app.setAdditionalProfiles("jdbc02"); // profile = profil ustawień
        app.run(args).close();
    }
}
