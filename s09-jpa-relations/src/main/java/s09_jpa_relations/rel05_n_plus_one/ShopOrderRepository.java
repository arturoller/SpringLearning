package s09_jpa_relations.rel05_n_plus_one;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Repozytorium zamówień — te same dane czytane na kilka sposobów, od N+1 do jednego zapytania. */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    /** PRZED: zwykłe zapytanie — klienci i pozycje zostają leniwe (pętla po nich = N+1). */
    List<ShopOrder> findAllByOrderByNumber();

    /** join fetch — klienci w tym samym SELECT. */
    @Query("select o from ShopOrder o join fetch o.customer order by o.number")
    List<ShopOrder> findAllWithCustomer();

    /** join fetch KOLEKCJI — pozycje w tym samym SELECT (wiersze zamówień powtarzają się w wyniku SQL). */
    @Query("select o from ShopOrder o join fetch o.lines order by o.number")
    List<ShopOrder> findAllWithLines();

    /**
     * {@code @EntityGraph} (graf encji) — to samo co join fetch klientów, ale bez pisania JPQL: Spring Data dokłada
     * do zapytania pochodnego graf „pobierz też customer”. Słowo „WithCustomerGraph” między find i By jest pomijane.
     */
    @EntityGraph(attributePaths = "customer")
    List<ShopOrder> findAllWithCustomerGraphByOrderByNumber();

    /**
     * ŹLE (pułapka z sekcji 7b lekcji): Page + join fetch kolekcji. Zapytanie liczące wylicza Spring Data — ze
     * złączeniem, więc liczy WIERSZE pozycji, a nie zamówienia.
     */
    @Query("select o from ShopOrder o join fetch o.lines")
    Page<ShopOrder> findPageWithLinesWrongCount(Pageable pageable);

    /** Poprawnie: własne zapytanie liczące (countQuery) — liczy same zamówienia. */
    @Query(value = "select o from ShopOrder o left join fetch o.lines",
            countQuery = "select count(o) from ShopOrder o")
    Page<ShopOrder> findPageWithLines(Pageable pageable);

    /** Projekcja DTO — tylko potrzebne kolumny, bez encji. */
    @Query("""
            select new s09_jpa_relations.rel05_n_plus_one.OrderSummary(o.number, c.name, c.city)
            from ShopOrder o join o.customer c
            order by o.number""")
    List<OrderSummary> findSummaries();
}
