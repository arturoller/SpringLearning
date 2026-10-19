package s10_transactions.tx05_programmatic_transactions;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium cennika (Spring Data JPA). */
public interface ProductRepository extends JpaRepository<Product, String> {
}
