package s09_jpa_relations.solutions.rel03_many_to_many;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s09_jpa_relations.rel03_many_to_many.Product;

/**
 * Klasa startowa testów rozwiązań Rel03. Encje są w pakiecie lekcji ({@code @EntityScan}); repozytoria: z tego
 * pakietu (SolutionProductRepository, SolutionProductSupplierRepository) i z pakietu lekcji (TagRepository…).
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
@EnableJpaRepositories(basePackageClasses = {SolutionProductRepository.class, Product.class})
public class Rel03SolutionsApplication {
}
