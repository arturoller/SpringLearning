package s10_transactions.tx02_propagation;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium zamówień (Spring Data JPA). */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
}
