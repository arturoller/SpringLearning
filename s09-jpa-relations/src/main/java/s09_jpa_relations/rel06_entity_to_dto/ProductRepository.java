package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium produktów. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);
}
