package s12_security.sec03_users_passwords;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Sec03UsersPasswords. Uruchom ▶ i wysyłaj zapytania z pliku
 * s12-security/http/sec03_users_passwords.http (anna, piotr — aktywni; marek — konto zablokowane).
 */
@SpringBootApplication
public class Sec03Application {

    public static void main(String[] args) {
        SpringApplication.run(Sec03Application.class, args);
    }
}
