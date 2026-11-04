package s11_testing.test01_test_slices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test01TestSlices. Wszystkie testy lekcji ({@code @WebMvcTest}, {@code @DataJpaTest},
 * {@code @JsonTest}, {@code @SpringBootTest}) znajdują ją same — szukają {@code @SpringBootConfiguration} w górę
 * drzewa pakietów. Profil {@code test01} wczytuje {@code application-test01.properties} (baza H2, Flyway).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s11-testing spring-boot:run -Dspring-boot.run.main-class=s11_testing.test01_test_slices.Test01Application}.
 * Gotowe zapytania: {@code s11-testing/http/test01_test_slices.http}.
 */
@SpringBootApplication
public class Test01Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test01Application.class);
        app.setAdditionalProfiles("test01"); // profile = profil ustawień
        app.run(args);
    }
}
