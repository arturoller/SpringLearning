package s08_data_jpa.jpa06_flyway_with_jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium produktów lekcji Jpa06FlywayWithJpa — dane pochodzą z migracji V3. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
}
