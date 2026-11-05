package s09_jpa_relations.solutions.rel04_lazy_loading;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s09_jpa_relations.rel04_lazy_loading.ShopOrder;

/**
 * Klasa startowa testów rozwiązań Rel04. Encje są w pakiecie lekcji ({@code @EntityScan}); repozytoria: z tego
 * pakietu (SolutionShopOrderRepository) i z pakietu lekcji (CustomerRepository…).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = ShopOrder.class)
@EnableJpaRepositories(basePackageClasses = {SolutionShopOrderRepository.class, ShopOrder.class})
public class Rel04SolutionsApplication {
}
