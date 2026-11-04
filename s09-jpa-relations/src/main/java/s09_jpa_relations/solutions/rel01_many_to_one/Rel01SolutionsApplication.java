package s09_jpa_relations.solutions.rel01_many_to_one;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s09_jpa_relations.rel01_many_to_one.CustomerRepository;
import s09_jpa_relations.rel01_many_to_one.ShopOrder;

/**
 * Klasa startowa testów rozwiązań Rel01. Encje są w pakiecie lekcji ({@code @EntityScan}); repozytoria: z tego
 * pakietu (SolutionShopOrderRepository) i z pakietu lekcji (CustomerRepository) — {@code @EnableJpaRepositories}
 * (enable = włącz) z obiema klasami-znacznikami pakietów.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = ShopOrder.class)
@EnableJpaRepositories(basePackageClasses = {SolutionShopOrderRepository.class, CustomerRepository.class})
public class Rel01SolutionsApplication {
}
