package s08_data_jpa.jpa06_flyway_with_jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repozytorium lekcji Jpa06FlywayWithJpa — tylko po to, żeby sprawdzić, że encja po migracji V3 działa. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByWeightGramsGreaterThanOrderByIdAsc(int minWeight);
}
