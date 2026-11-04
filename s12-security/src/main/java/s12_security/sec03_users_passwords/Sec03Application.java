package s12_security.sec03_users_passwords;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec03UsersPasswords. Testy lekcji znajdują ją same (ten sam pakiet).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s12-security spring-boot:run -Dspring-boot.run.main-class=s12_security.sec03_users_passwords.Sec03Application}.
 * Gotowe zapytania: {@code s12-security/http/sec03_users_passwords.http}.
 * <p>
 * Profil {@code sec03} (plik {@code application-sec03.properties}) włącza bazę {@code jdbc:h2:mem:sec03} i migracje Flyway
 * z {@code db/migration/sec03}. Włączamy go tutaj, w {@code main}, a w testach przez {@code @ActiveProfiles("sec03")} —
 * dzięki temu baza tej lekcji nie wpływa na inne lekcje działu.
 */
@SpringBootApplication
public class Sec03Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Sec03Application.class);
        application.setAdditionalProfiles("sec03");   // profile = profil, zestaw ustawień włączany po nazwie
        application.run(args);
    }
}
