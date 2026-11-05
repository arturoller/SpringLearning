package s09_jpa_relations.rel01_many_to_one;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Repozytorium „szkiełka” EagerShopOrder (sekcja 2 lekcji: domyślne EAGER i N+1). */
public interface EagerShopOrderRepository extends JpaRepository<EagerShopOrder, Long> {

    @Query("select o from EagerShopOrder o order by o.orderNumber")
    List<EagerShopOrder> findAllOrdered();
}
