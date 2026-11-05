package s08_data_jpa.solutions.jpa01_entities;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import s08_data_jpa.jpa01_entities.Product;

/**
 * Klasa startowa testów rozwiązań Jpa01 (wycinek {@code @DataJpaTest} szuka {@code @SpringBootConfiguration} w górę
 * pakietów). Encje lekcji leżą w INNYM pakiecie (jpa01_entities), a Boot domyślnie skanuje encje tylko w pakiecie
 * klasy startowej — dlatego {@code @EntityScan} (entity scan = skanowanie encji) wskazuje pakiet lekcji.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
public class Jpa01SolutionsApplication {
}
