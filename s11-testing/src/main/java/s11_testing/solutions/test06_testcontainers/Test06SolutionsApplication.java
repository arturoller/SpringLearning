package s11_testing.solutions.test06_testcontainers;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s11_testing.test06_testcontainers.Product;
import s11_testing.test06_testcontainers.ProductRepository;

/**
 * Konfiguracja testów rozwiązań Test06 (testy szukają {@code @SpringBootConfiguration} w górę pakietów). Encje
 * i repozytoria lekcji leżą w INNYM pakiecie, dlatego {@code @EntityScan} i {@code @EnableJpaRepositories} wskazują
 * pakiet lekcji. Serwis i kontroler testy dokładają przez {@code @Import}.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
@EnableJpaRepositories(basePackageClasses = ProductRepository.class)
public class Test06SolutionsApplication {
}
