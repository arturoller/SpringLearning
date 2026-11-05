package s09_jpa_relations.rel02_one_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium zamówień lekcji Rel02OneToMany. Pozycje zapisują się KASKADOWO razem z zamówieniem (sekcja 3). */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    Optional<ShopOrder> findByOrderNumber(String orderNumber);
}
