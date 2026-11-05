package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Repozytorium zamówień lekcji Rel06EntityToDto — zapytania pod konkretne DTO (SQL w lekcji, sprawdzony testem). */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    Optional<ShopOrder> findByOrderNumber(String orderNumber);

    // Pod OrderDetailsDto: wszystko, czego potrzebuje maper, jednym zapytaniem (left — zamówienie może być puste)
    @Query("select o from ShopOrder o join fetch o.customer left join fetch o.lines l left join fetch l.product"
            + " where o.orderNumber = :orderNumber")
    Optional<ShopOrder> findDetailsByOrderNumber(String orderNumber);

    // Pod OrderSummaryDto: projekcja prosto do rekordu, suma liczona w bazie
    @Query("""
            select new s09_jpa_relations.rel06_entity_to_dto.OrderSummaryDto(
                o.orderNumber, c.name, o.status, coalesce(sum(l.quantity * l.unitPrice), 0))
            from ShopOrder o join o.customer c left join o.lines l
            group by o.orderNumber, c.name, o.status
            order by o.orderNumber""")
    List<OrderSummaryDto> findSummaries();
}
