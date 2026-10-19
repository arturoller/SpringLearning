package s04_web_rest.solutions.rest02_request_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa rozwiązań lekcji Rest02RequestData — potrzebna testom rozwiązań ({@code @WebMvcTest} szuka
 * {@code @SpringBootConfiguration} w górę drzewa pakietów). Skanuje tylko pakiet solutions.rest02_request_data.
 */
@SpringBootApplication
public class Rest02SolutionsApplication {

    public static void main(String[] args) {
        SpringApplication.run(Rest02SolutionsApplication.class, args);
    }
}
