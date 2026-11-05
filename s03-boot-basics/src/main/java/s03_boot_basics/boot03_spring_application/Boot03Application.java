package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Boot03SpringApplication.
 * <p>
 * Skanuje swój pakiet, więc znajduje {@link ShopStartupLog}, {@link PlainArgumentsRunner},
 * {@link OptionArgumentsRunner} i {@link LazyReportGenerator}. Aplikacja nie ma serwera WWW: buduje kontekst,
 * wykonuje runnery (runner = „biegacz” — kod uruchamiany zaraz po starcie) i kończy działanie.
 * <p>
 * Uruchom ▶ {@code main} z argumentami, np. {@code --shop.mode=demo zeszyt dlugopis}, i zobacz w logu linie
 * runnerów. Metoda {@code main} to dokładnie to, co w testach robimy ręcznie krok po kroku.
 */
@SpringBootApplication
public class Boot03Application {

    public static void main(String[] args) {
        SpringApplication.run(Boot03Application.class, args);
    }
}
