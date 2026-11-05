package s07_data_jdbc.jdbc03_sql_init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc03SqlInit (bez weba). Profil {@code jdbc03} wczytuje
 * {@code application-jdbc03.properties} — bazę, schemat i dane tej lekcji.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jdbc03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jdbc03Application.class);
        app.setAdditionalProfiles("jdbc03"); // profile = profil ustawień
        app.run(args).close();
    }
}
