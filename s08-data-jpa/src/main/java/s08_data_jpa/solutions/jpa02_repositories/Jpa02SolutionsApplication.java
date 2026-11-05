package s08_data_jpa.solutions.jpa02_repositories;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import s08_data_jpa.jpa02_repositories.Product;
import s08_data_jpa.jpa02_repositories.ProductRepository;

/**
 * Klasa startowa testów rozwiązań Jpa02 (wycinek {@code @DataJpaTest} szuka {@code @SpringBootConfiguration} w górę
 * pakietów). Encja i repozytorium lekcji leżą w INNYM pakiecie, a Boot domyślnie skanuje tylko pakiet klasy
 * startowej — dlatego {@code @EntityScan} (encje) i {@code @EnableJpaRepositories} (repozytoria) wskazują pakiet lekcji.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Product.class)
@EnableJpaRepositories(basePackageClasses = ProductRepository.class) // enable = włącz
public class Jpa02SolutionsApplication {
}
