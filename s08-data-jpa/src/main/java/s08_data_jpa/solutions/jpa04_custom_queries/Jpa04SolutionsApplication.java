package s08_data_jpa.solutions.jpa04_custom_queries;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa04_custom_queries.Product;

/**
 * Klasa startowa testów rozwiązań Jpa04. Repozytorium z rozwiązaniami (SolutionProductRepository) leży w TYM pakiecie,
 * więc Boot znajdzie je sam; encja Product jest w pakiecie lekcji — stąd {@code @EntityScan}.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class Jpa04SolutionsApplication {
}
