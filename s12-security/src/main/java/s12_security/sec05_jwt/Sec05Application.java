package s12_security.sec05_jwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec05Jwt. Testy lekcji znajdują ją same (ten sam pakiet).
 * <p>
 * Uruchomienie: ▶ obok {@code main} albo
 * {@code ./mvnw -pl s12-security spring-boot:run -Dspring-boot.run.main-class=s12_security.sec05_jwt.Sec05Application}.
 * Gotowe zapytania: {@code s12-security/http/sec05_jwt.http} (najpierw pobierz token, potem używaj go w nagłówku
 * {@code Authorization: Bearer …}). Użytkownicy (TYLKO DO NAUKI, hasło zawsze {@code haslo-testowe}): {@code anna} (CUSTOMER),
 * {@code ewa} (EMPLOYEE), {@code admin} (ADMIN).
 * <p>
 * Klucz do podpisywania tokenów jest losowany przy KAŻDYM starcie ({@link Sec05SecurityConfig#jwtSecretKey}) — po restarcie
 * stare tokeny przestają działać. Na produkcji klucz przychodzi z sekretów środowiska (s19_deploy).
 */
@SpringBootApplication
public class Sec05Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec05Application.class, args);
    }
}
