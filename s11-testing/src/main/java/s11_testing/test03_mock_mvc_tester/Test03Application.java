package s11_testing.test03_mock_mvc_tester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test03MockMvcTester. Testy lekcji znajdują ją same. Uruchomiona (▶) wystawia API produktów
 * na porcie 8080 — gotowe zapytania: s11-testing/http/test03_mock_mvc_tester.http (te same, które testy wysyłają
 * przez MockMvcTester, tylko „ręcznie”).
 */
@SpringBootApplication
public class Test03Application {

    public static void main(String[] args) {
        SpringApplication.run(Test03Application.class, args);
    }
}
