package s10_transactions.tx04_read_only_isolation;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium produktów z wersją (Spring Data JPA). */
public interface VersionedProductRepository extends JpaRepository<VersionedProduct, String> {
}
