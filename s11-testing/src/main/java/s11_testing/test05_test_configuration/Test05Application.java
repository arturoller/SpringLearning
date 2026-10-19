package s11_testing.test05_test_configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/**
 * Klasa startowa lekcji Test05TestConfiguration — konfiguracja, którą znajdują testy w pakiecie lekcji.
 * Definiuje bean {@code clock} (zegar systemowy): w produkcji „teraz” to prawdziwe teraz, a test podmienia go na
 * zegar zatrzymany ({@code Clock.fixed}). Aplikacja nie ma kontrolerów — „wynikiem” lekcji są testy.
 */
@SpringBootApplication
public class Test05Application {

    /** Zegar aplikacji. Każdy bean, który potrzebuje „teraz”, dostaje go przez konstruktor (CLAUDE.md, sekcja 5). */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    public static void main(String[] args) {
        SpringApplication.run(Test05Application.class, args);
    }
}
