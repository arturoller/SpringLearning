package s09_jpa_relations.solutions.rel06_entity_to_dto;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s09_jpa_relations.rel06_entity_to_dto.ShopOrder;

/**
 * Klasa startowa testów rozwiązań Rel06. Encje są w pakiecie lekcji ({@code @EntityScan}); repozytoria: z tego
 * pakietu (SolutionShopOrderRepository) i z pakietu lekcji (CustomerRepository, ProductRepository…).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = ShopOrder.class)
@EnableJpaRepositories(basePackageClasses = {SolutionShopOrderRepository.class, ShopOrder.class})
public class Rel06SolutionsApplication {
}
