package s08_data_jpa.solutions.jpa03_query_methods;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa03_query_methods.Product;

/**
 * Klasa startowa testów rozwiązań Jpa03. Repozytorium z rozwiązaniami (SolutionProductRepository) leży w TYM pakiecie,
 * więc Boot znajdzie je sam; encja Product jest w pakiecie lekcji — stąd {@code @EntityScan}.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class Jpa03SolutionsApplication {
}
