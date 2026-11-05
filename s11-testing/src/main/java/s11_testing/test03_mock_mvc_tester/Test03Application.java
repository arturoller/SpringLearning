package s11_testing.test03_mock_mvc_tester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test03MockMvcTester. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują
 * ją same. Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s11-testing spring-boot:run -Dspring-boot.run.main-class=s11_testing.test03_mock_mvc_tester.Test03Application}.
 * Gotowe zapytania: {@code s11-testing/http/test03_mock_mvc_tester.http}.
 */
@SpringBootApplication
public class Test03Application {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Test03Application.class);
        app.setAdditionalProfiles("test03"); // profile = profil ustawień
        app.run(args);
    }
}
