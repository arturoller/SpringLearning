package s09_jpa_relations.solutions.rel05_n_plus_one;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s09_jpa_relations.rel05_n_plus_one.CustomerSpending;
import s09_jpa_relations.rel05_n_plus_one.ShopOrder;

import java.util.Collection;
import java.util.List;

/** Zapytania do rozwiązań wzorcowych ćwiczeń lekcji Rel05NPlusOne (odpowiednik ExerciseShopOrderRepository). */
public interface SolutionShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Ćwiczenie 1: klient „do jednego” — zwykły join fetch, wierszy tyle co zamówień
    @Query("select o from ShopOrder o join fetch o.customer order by o.orderNumber")
    List<ShopOrder> findAllWithCustomer();

    // Ćwiczenie 2: LEFT — inaczej puste ZAM-010 by zniknęło
    @Query("select o from ShopOrder o left join fetch o.lines l left join fetch l.product order by o.orderNumber, l.id")
    List<ShopOrder> findAllWithLinesAndProducts();

    // Ćwiczenie 3: ścieżka "lines.product" — graf sięga dwa poziomy w głąb
    @EntityGraph(attributePaths = {"lines", "lines.product"})
    List<ShopOrder> findByCustomerIdOrderByOrderNumber(Long customerId);

    // Ćwiczenie 4: count(distinct o) — JOIN z pozycjami powtarza zamówienie w kilku wierszach
    @Query("""
            select new s09_jpa_relations.rel05_n_plus_one.CustomerSpending(
                c.name, count(distinct o), coalesce(sum(l.quantity * l.unitPrice), 0))
            from ShopOrder o join o.customer c left join o.lines l
            group by c.name
            order by c.name""")
    List<CustomerSpending> spendingPerCustomer();

    // Ćwiczenie 5: dwa zapytania zamiast jednego z dwiema listami (MultipleBagFetchException)
    @Query("select o from ShopOrder o left join fetch o.lines order by o.orderNumber")
    List<ShopOrder> findAllWithLines();

    @Query("select o from ShopOrder o left join fetch o.payments where o in :orders")
    List<ShopOrder> fetchPayments(Collection<ShopOrder> orders);
}
