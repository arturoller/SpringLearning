package s09_jpa_relations.rel03_many_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repozytorium encji pośredniej ProductSupplier (sekcja 4 lekcji). */
public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {

    List<ProductSupplier> findByProductIdOrderByPurchasePrice(Long productId);
}
