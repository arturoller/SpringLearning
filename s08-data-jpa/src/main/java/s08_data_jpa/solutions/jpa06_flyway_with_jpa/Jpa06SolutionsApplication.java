package s08_data_jpa.solutions.jpa06_flyway_with_jpa;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Konfiguracja dla testów rozwiązań lekcji Jpa06FlywayWithJpa. Skanuje TYLKO pakiet rozwiązań (encja Customer
 * z rozwiązania). Z profilem jpa06 Flyway tworzy te same tabele co w lekcji, a validate sprawdza jedyną encję —
 * Customer; tabela products bez encji mu nie przeszkadza.
 */
@SpringBootApplication
public class Jpa06SolutionsApplication {
}
