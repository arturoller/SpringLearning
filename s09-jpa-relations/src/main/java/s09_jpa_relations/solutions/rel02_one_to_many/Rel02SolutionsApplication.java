package s09_jpa_relations.solutions.rel02_one_to_many;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s09_jpa_relations.rel02_one_to_many.ShopOrder;

/**
 * Klasa startowa testów rozwiązań Rel02. Encje są w pakiecie lekcji ({@code @EntityScan}); repozytoria: z tego
 * pakietu (SolutionOrderLineRepository) i z pakietu lekcji (ShopOrderRepository, ProductRepository…).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = ShopOrder.class)
@EnableJpaRepositories(basePackageClasses = {SolutionOrderLineRepository.class, ShopOrder.class})
public class Rel02SolutionsApplication {
}
