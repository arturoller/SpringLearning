package s11_testing.solutions.test04_data_jpa_test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s11_testing.test04_data_jpa_test.Product;
import s11_testing.test04_data_jpa_test.ProductRepository;

/**
 * Klasa startowa testów rozwiązań Test04 (wycinek {@code @DataJpaTest} szuka {@code @SpringBootConfiguration} w górę
 * pakietów). Encje i repozytoria lekcji leżą w INNYM pakiecie, a Boot domyślnie skanuje tylko pakiet klasy startowej —
 * dlatego {@code @EntityScan} (encje) i {@code @EnableJpaRepositories} (repozytoria) wskazują pakiet lekcji.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
@EnableJpaRepositories(basePackageClasses = ProductRepository.class) // enable = włącz
public class Test04SolutionsApplication {
}
