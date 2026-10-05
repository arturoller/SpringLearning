package s00_start.start03_first_application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Start03FirstApplication. Uruchom ją: ▶ obok metody main, poczekaj na „Tomcat started on port
 * 8080” i otwórz http://localhost:8080/api/hello?name=Ala albo plik s00-start/http/start03_first_application.http.
 *
 * {@code @SpringBootApplication} = trzy adnotacje w jednej:
 *   {@code @Configuration} (ta klasa może definiować beany), {@code @EnableAutoConfiguration} (włącz autokonfigurację),
 *   {@code @ComponentScan} (skanuj TEN pakiet i jego podpakiety w poszukiwaniu klas z {@code @Component}, {@code @Service}...).
 */
@SpringBootApplication
public class Start03Application {

    public static void main(String[] args) {
        // run = uruchom: tworzy kontekst, skanuje komponenty, startuje Tomcata i czeka na zapytania HTTP
        SpringApplication.run(Start03Application.class, args);
    }
}
