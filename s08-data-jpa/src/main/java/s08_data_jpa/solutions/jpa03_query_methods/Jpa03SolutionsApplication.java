package s08_data_jpa.solutions.jpa03_query_methods;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa03_query_methods.Product;

/**
 * Konfiguracja dla testów rozwiązań lekcji Jpa03QueryMethods. Repozytoria skanuje w SWOIM pakiecie
 * (ProductSolutionRepository), a encje — w pakiecie lekcji: {@code @EntityScan} (entity scan = skanowanie encji)
 * wskazuje pakiet klasy Product, bo rozwiązania pracują na encji z lekcji.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class Jpa03SolutionsApplication {
}
