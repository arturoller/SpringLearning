package s08_data_jpa.jpa03_query_methods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa03QueryMethods (profil „jpa03”: baza jdbc:h2:mem:jpa03, wypisywanie SQL).
 * Przy starcie Spring Data SPRAWDZA wszystkie metody zapytań w ProductRepository — błąd w nazwie zatrzyma start.
 */
@SpringBootApplication
public class Jpa03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa03Application.class);
        app.setAdditionalProfiles("jpa03");
        app.run(args);
    }
}
