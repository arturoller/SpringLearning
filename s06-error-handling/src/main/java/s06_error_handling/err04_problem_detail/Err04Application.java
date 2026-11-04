package s06_error_handling.err04_problem_detail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Err04ProblemDetail. Testy lekcji ({@code @WebMvcTest}, {@code @SpringBootTest}) znajdują ją same.
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s06-error-handling spring-boot:run -Dspring-boot.run.main-class=s06_error_handling.err04_problem_detail.Err04Application}.
 * Gotowe zapytania: {@code s06-error-handling/http/err04_problem_detail.http}.
 * <p>
 * Profil {@code err04} (plik {@code application-err04.properties}) włączamy tutaj, w {@code main}, a w testach przez
 * {@code @ActiveProfiles("err04")} — dzięki temu ustawienia tej lekcji (własne komunikaty błędów) nie wpływają na inne lekcje.
 */
@SpringBootApplication
public class Err04Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Err04Application.class);
        application.setAdditionalProfiles("err04");   // profile = profil, zestaw ustawień włączany po nazwie
        application.run(args);
    }
}
