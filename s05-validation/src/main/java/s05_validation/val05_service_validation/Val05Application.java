package s05_validation.val05_service_validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Val05ServiceValidation. Lekcja nie ma kontrolerów — walidację metod serwisu pokazują testy
 * ({@code @SpringBootTest}). Uwaga: {@link ShopProperties} NIE jest tu rejestrowane (wymaga ustawień), używa go tylko
 * test z {@code ApplicationContextRunner}.
 */
@SpringBootApplication
public class Val05Application {

    public static void main(String[] args) {
        SpringApplication.run(Val05Application.class, args);
    }
}
