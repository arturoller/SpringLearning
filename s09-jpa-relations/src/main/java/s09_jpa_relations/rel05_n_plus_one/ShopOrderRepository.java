package s09_jpa_relations.rel05_n_plus_one;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

/**
 * Repozytorium zamówień lekcji Rel05NPlusOne — od zapytania z problemem N+1 do zapytań bez niego.
 * SQL i liczba zapytań każdej metody (sprawdzone testem Rel05NPlusOneTest) są w lekcji.
 */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Sekcja 1: zwykła lista — relacje leniwe, więc dostęp do klienta/pozycji każdego zamówienia = N+1.
    @Query("select o from ShopOrder o order by o.orderNumber")
    List<ShopOrder> findAllOrdered();

    // Sekcja 2: klienci od razu (relacja „do jednego” — liczba wierszy się nie zmienia).
    @Query("select o from ShopOrder o join fetch o.customer order by o.orderNumber")
    List<ShopOrder> findAllWithCustomer();

    // Sekcja 2: klienci + pozycje + produkty jednym zapytaniem. LEFT join fetch — żeby puste ZAM-010 nie zniknęło.
    @Query("select o from ShopOrder o join fetch o.customer left join fetch o.lines l left join fetch l.product"
            + " order by o.orderNumber")
    List<ShopOrder> findAllWithCustomerAndLines();

    // Sekcja 2: DOBRA PRAKTYKA przy stronicowaniu — krok 1: strona samych id (stronicuje BAZA: offset/fetch first)…
    @Query("select o.id from ShopOrder o order by o.orderNumber")
    List<Long> findIdsPage(Pageable pageable);

    // …krok 2: pełne encje z pozycjami tylko dla tych id (bez stronicowania, więc join fetch jest bezpieczny).
    @Query("select o from ShopOrder o left join fetch o.lines where o.id in :ids order by o.orderNumber")
    List<ShopOrder> findWithLinesByIdIn(Collection<Long> ids);

    // Sekcja 3: @EntityGraph z listą atrybutów — zapytanie z nazwy metody, graf dokłada JOIN-y.
    @EntityGraph(attributePaths = {"customer", "lines"})
    List<ShopOrder> findByCustomerIdOrderByOrderNumber(Long customerId);

    // Sekcja 3: ten sam efekt nazwanym grafem z encji (@NamedEntityGraph w ShopOrder).
    @EntityGraph("ShopOrder.withCustomerAndLines")
    List<ShopOrder> findAllByOrderByOrderNumber();

    // Sekcja 4: druga lista „dociągnięta” do zamówień JUŻ wczytanych w tym kontekście — osobnym zapytaniem.
    @Query("select o from ShopOrder o left join fetch o.payments where o in :orders")
    List<ShopOrder> fetchPayments(Collection<ShopOrder> orders);

    // Sekcja 5: projekcja DTO — jedno zapytanie z JOIN i GROUP BY, bez encji.
    @Query("""
            select new s09_jpa_relations.rel05_n_plus_one.OrderRow(
                o.orderNumber, c.name, count(l), coalesce(sum(l.quantity * l.unitPrice), 0))
            from ShopOrder o join o.customer c left join o.lines l
            group by o.orderNumber, c.name
            order by o.orderNumber""")
    List<OrderRow> findOrderRows();
}
