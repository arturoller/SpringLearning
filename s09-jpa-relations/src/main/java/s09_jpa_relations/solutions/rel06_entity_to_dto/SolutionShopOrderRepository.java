package s09_jpa_relations.solutions.rel06_entity_to_dto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s09_jpa_relations.rel06_entity_to_dto.OrderSummaryDto;
import s09_jpa_relations.rel06_entity_to_dto.ShopOrder;

import java.util.List;

/** Zapytania do rozwiązań wzorcowych ćwiczeń lekcji Rel06EntityToDto (odpowiednik ExerciseShopOrderRepository). */
public interface SolutionShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Ćwiczenie 2: projekcja jednego klienta — warunek na id klienta, grupowanie jak w findSummaries
    @Query("""
            select new s09_jpa_relations.rel06_entity_to_dto.OrderSummaryDto(
                o.orderNumber, c.name, o.status, coalesce(sum(l.quantity * l.unitPrice), 0))
            from ShopOrder o join o.customer c left join o.lines l
            where c.id = :customerId
            group by o.orderNumber, c.name, o.status
            order by o.orderNumber""")
    List<OrderSummaryDto> findSummariesOfCustomer(Long customerId);

    // Ćwiczenie 4: wszystko pod maper jednym zapytaniem
    @Query("""
            select o from ShopOrder o join fetch o.customer left join fetch o.lines l left join fetch l.product
            where o.customer.id = :customerId
            order by o.orderNumber, l.id""")
    List<ShopOrder> findDetailsOfCustomer(Long customerId);
}
