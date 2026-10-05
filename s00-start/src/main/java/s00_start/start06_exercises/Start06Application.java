package s00_start.start06_exercises;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Start06Exercises. Skanuje TYLKO pakiet s00_start.start06_exercises — rozwiązania wzorcowe
 * (pakiet s00_start.solutions.start06_exercises) są poza jej zasięgiem i mają własną klasę startową.
 */
@SpringBootApplication
public class Start06Application {

    public static void main(String[] args) {
        SpringApplication.run(Start06Application.class, args);
    }
}
