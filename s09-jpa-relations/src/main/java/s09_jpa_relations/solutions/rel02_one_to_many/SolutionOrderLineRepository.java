package s09_jpa_relations.solutions.rel02_one_to_many;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s09_jpa_relations.rel02_one_to_many.OrderLine;
import s09_jpa_relations.rel02_one_to_many.ProductQuantity;

import java.math.BigDecimal;
import java.util.List;

/** Zapytania do rozwiązań wzorcowych lekcji Rel02OneToMany (odpowiednik ExerciseOrderLineRepository). */
public interface SolutionOrderLineRepository extends JpaRepository<OrderLine, Long> {

    // Rozwiązanie 3: coalesce zamienia null (brak pozycji) na 0; l.order.id = sama kolumna order_id (bez joina)
    @Query("select coalesce(sum(l.quantity * l.unitPrice), 0) from OrderLine l where l.order.id = :orderId")
    BigDecimal totalOfOrder(Long orderId);

    // Rozwiązanie 4: JOIN do product przez l.product.name, grupowanie po nazwie; rekord bez "new"
    @Query("select l.product.name, sum(l.quantity) from OrderLine l group by l.product.name order by l.product.name")
    List<ProductQuantity> sumQuantityPerProduct();
}
