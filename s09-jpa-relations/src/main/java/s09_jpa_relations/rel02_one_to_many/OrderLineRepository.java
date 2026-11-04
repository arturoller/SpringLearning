package s09_jpa_relations.rel02_one_to_many;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

/** Repozytorium pozycji lekcji Rel02OneToMany — zapytanie agregujące z sekcji 5. */
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    // Wartość zamówienia policzona przez BAZĘ — jedno zapytanie, zero wczytanych encji (sprawdzone testem):
    // select coalesce(sum((ol1_0.quantity*ol1_0.unit_price)),0) from order_line ol1_0 where ol1_0.order_id=?
    @Query("select coalesce(sum(l.quantity * l.unitPrice), 0) from OrderLine l where l.order.id = :orderId")
    BigDecimal sumTotalOfOrder(Long orderId);

    long countByOrderId(Long orderId);
}
