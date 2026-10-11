package s08_data_jpa.jpa04_custom_queries;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Jpa04CustomQueries (profil „jpa04”: baza jdbc:h2:mem:jpa04, wypisywanie SQL).
 * Przy starcie Hibernate sprawdza składnię każdego zapytania JPQL z {@code @Query} — natywnego SQL nie sprawdza.
 */
@SpringBootApplication
public class Jpa04Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Jpa04Application.class);
        app.setAdditionalProfiles("jpa04");
        app.run(args);
    }
}
