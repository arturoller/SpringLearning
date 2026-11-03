package s03_boot_basics.boot01_auto_configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Boot01AutoConfiguration.
 * <p>
 * Celowo PUSTA: nie ma tu ani jednej metody {@code @Bean} (bean = ziarno — obiekt zarządzany przez Springa).
 * Mimo to po starcie w kontekście są {@code DataSource}, {@code JdbcTemplate} i {@code JdbcClient} — utworzyła je
 * autokonfiguracja (auto-configuration), bo na classpath (ścieżce klas) leżą starter JDBC i baza H2.
 * <p>
 * Uruchom ▶ {@code main} z argumentem {@code --debug}, żeby zobaczyć w konsoli CONDITIONS EVALUATION REPORT
 * (raport oceny warunków) — sekcja 4 lekcji. Aplikacja nie ma serwera WWW: buduje kontekst i kończy działanie.
 */
@SpringBootApplication // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan (sekcja 1 lekcji)
public class Boot01Application {

    public static void main(String[] args) {
        SpringApplication.run(Boot01Application.class, args);
    }
}
