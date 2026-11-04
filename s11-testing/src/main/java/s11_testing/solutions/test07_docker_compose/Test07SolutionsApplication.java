package s11_testing.solutions.test07_docker_compose;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s11_testing.test07_docker_compose.Product;
import s11_testing.test07_docker_compose.ProductController;
import s11_testing.test07_docker_compose.ProductRepository;

/**
 * Konfiguracja testów rozwiązań Test07 (testy szukają {@code @SpringBootConfiguration} w górę pakietów). Encje,
 * repozytorium i kontroler lekcji leżą w INNYM pakiecie — wskazujemy je jawnie.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
@EnableJpaRepositories(basePackageClasses = ProductRepository.class)
@Import(ProductController.class)
public class Test07SolutionsApplication {
}
