package s07_data_jdbc.jdbc01_jdbc_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jdbc01JdbcClient (bez weba: buduje kontekst z bazą H2 i kończy działanie).
 * Profil {@code jdbc01} wczytuje {@code application-jdbc01.properties} — bazę, schemat i dane tej lekcji.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
public class Jdbc01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jdbc01Application.class);
        app.setAdditionalProfiles("jdbc01"); // profile = profil ustawień
        app.run(args).close();
    }
}
