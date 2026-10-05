package s00_start.start04_testing_intro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Start04TestingIntro. Testy tej lekcji ({@code @SpringBootTest}, {@code @WebMvcTest}) znajdują ją
 * same — szukają klasy z {@code @SpringBootApplication} w pakiecie testu i wyżej.
 * Można ją też uruchomić i wysłać zapytanie {@code GET http://localhost:8080/api/price?net=10000&vat=23}
 * — gotowe zapytania są w pliku s00-start/http/start04_testing_intro.http.
 */
@SpringBootApplication
public class Start04Application {

    public static void main(String[] args) {
        SpringApplication.run(Start04Application.class, args);
    }
}
