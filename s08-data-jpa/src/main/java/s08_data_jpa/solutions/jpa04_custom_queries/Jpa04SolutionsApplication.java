package s08_data_jpa.solutions.jpa04_custom_queries;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa04_custom_queries.Product;

/**
 * Konfiguracja dla testów rozwiązań lekcji Jpa04CustomQueries: repozytoria z tego pakietu,
 * encje z pakietu lekcji ({@code @EntityScan} wskazuje pakiet klasy Product).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class Jpa04SolutionsApplication {
}
