package s03_boot_basics.boot03_spring_application;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * PRZYKŁAD BŁĘDU (pytanie kontrolne 4): runner, który rzuca wyjątek kontrolowany (checked) IOException — błąd
 * wejścia/wyjścia, np. uszkodzony plik. Bez {@code @Configuration} — dokłada go tylko test
 * (jako dodatkowe źródło SpringApplicationBuilder), żeby nie psuł zwykłego startu Boot03Application.
 */
public class Boot03FailingRunnerConfig {

    @Bean
    CommandLineRunner brokenImportRunner() {
        return args -> {
            throw new IOException("Plik cennika jest uszkodzony");
        };
    }
}
