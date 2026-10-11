package s09_jpa_relations.rel02_one_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium zamówień. Pozycje nie mają własnego repozytorium — zapisują się kaskadą razem z zamówieniem. */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
}
