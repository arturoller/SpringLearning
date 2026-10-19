package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium stanów magazynowych (Spring Data JPA). */
public interface StockItemRepository extends JpaRepository<StockItem, String> {
}
