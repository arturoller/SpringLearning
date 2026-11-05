package s07_data_jdbc.solutions.jdbc05_repository;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa testów rozwiązań Jdbc05 (potrzebna wycinkowi @JdbcTest, który szuka @SpringBootConfiguration
 * w górę pakietów). Rozwiązania nie są beanami; repozytorium test dokłada przez @Import. Schemat i dane
 * bierze z profilu jdbc05 (migracje Flyway lekcji).
 */
@SpringBootApplication
public class Jdbc05SolutionsApplication {
}
