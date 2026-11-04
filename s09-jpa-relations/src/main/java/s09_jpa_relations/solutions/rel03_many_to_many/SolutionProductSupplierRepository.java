package s09_jpa_relations.solutions.rel03_many_to_many;

import org.springframework.data.jpa.repository.JpaRepository;
import s09_jpa_relations.rel03_many_to_many.ProductSupplier;

import java.util.Optional;

/** Rozwiązanie 5: metoda z nazwy na encji pośredniej — pierwszy wiersz po sortowaniu ceną rosnąco (SQL: {@code … order by ps1_0.purchase_price fetch first ? rows only}). */
public interface SolutionProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {

    Optional<ProductSupplier> findFirstByProductIdOrderByPurchasePriceAsc(Long productId);
}
