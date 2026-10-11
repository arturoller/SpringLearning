package s11_testing.test01_test_slices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test01TestSlices. Testy tej lekcji (wycinki i {@code @SpringBootTest}) znajdują ją same —
 * szukają klasy z {@code @SpringBootConfiguration} w pakiecie testu i wyżej. Baza: H2 w pamięci, tabele z encji
 * (ddl-auto=create-drop — domyślne dla bazy wbudowanej). Uruchomiona: serwer na porcie 8080, zapytania w
 * s11-testing/http/test01_test_slices.http (baza jest pusta, więc zobaczysz 404 i pustą listę).
 */
@SpringBootApplication
public class Test01Application {

    public static void main(String[] args) {
        SpringApplication.run(Test01Application.class, args);
    }
}
