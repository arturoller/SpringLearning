package s11_testing.test02_mockito_beans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test02MockitoBeans. Testy lekcji ją znajdują same (szukają w górę drzewa pakietów).
 * Profil {@code test02} wczytuje {@code application-test02.properties} (adres zewnętrznej usługi cen).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s11-testing spring-boot:run -Dspring-boot.run.main-class=s11_testing.test02_mockito_beans.Test02Application}.
 * Gotowe zapytania: {@code s11-testing/http/test02_mockito_beans.http}.
 */
@SpringBootApplication
public class Test02Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test02Application.class);
        app.setAdditionalProfiles("test02"); // profile = profil ustawień
        app.run(args);
    }
}
