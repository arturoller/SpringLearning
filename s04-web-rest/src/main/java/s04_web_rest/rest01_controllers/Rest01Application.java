package s04_web_rest.rest01_controllers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest01Controllers. Testy lekcji ({@code @WebMvcTest}) znajdują ją same — szukają klasy
 * z {@code @SpringBootApplication} w pakiecie testu i wyżej.
 * <p>
 * Jak uruchomić aplikację: ▶ obok {@code main} w IntelliJ albo
 * {@code ./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=s04_web_rest.rest01_controllers.Rest01Application}.
 * Poczekaj na „Tomcat started on port 8080” i otwórz plik {@code s04-web-rest/http/rest01_controllers.http}.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan (s03_boot_basics/Boot01AutoConfiguration)
public class Rest01Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest01Application.class, args);
    }
}
