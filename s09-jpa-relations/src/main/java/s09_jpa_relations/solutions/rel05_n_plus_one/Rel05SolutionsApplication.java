package s09_jpa_relations.solutions.rel05_n_plus_one;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s09_jpa_relations.rel05_n_plus_one.ShopOrder;

/**
 * Klasa startowa testów rozwiązań Rel05. Encje są w pakiecie lekcji ({@code @EntityScan}); repozytorium tylko
 * z tego pakietu (SolutionShopOrderRepository) — rozwiązania nie potrzebują repozytoriów lekcji.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = ShopOrder.class)
@EnableJpaRepositories(basePackageClasses = SolutionShopOrderRepository.class)
public class Rel05SolutionsApplication {
}
