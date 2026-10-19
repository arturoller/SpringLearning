package s11_testing.test02_mockito_beans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Test02MockitoBeans — konfiguracja, którą znajdują testy z {@code @SpringBootTest}
 * w pakiecie lekcji. Aplikacja nie ma kontrolerów; „wynikiem” lekcji są testy.
 */
@SpringBootApplication
public class Test02Application {

    public static void main(String[] args) {
        SpringApplication.run(Test02Application.class, args);
    }
}
