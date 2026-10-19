package s21_capstone.cap01_shop_api.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Repozytorium zamówień: dwa zapytania „pod ekran” — lista (projekcja) i szczegóły (join fetch). */
public interface OrderRepository extends JpaRepository<ShopOrder, Long> {

    /**
     * LISTA zamówień: projekcja DTO ze stronicowaniem. Jedno zapytanie na stronę (+ jedno {@code count}, gdy Spring
     * Data potrzebuje liczby wszystkich wierszy) — niezależnie od liczby zamówień i pozycji.
     * {@code countQuery} podajemy jawnie. Bez niego Spring Data i Hibernate też policzą poprawnie, ale przez
     * podzapytanie: {@code select count(*) from (select ... join ... left join ... group by ...)} — z tymi samymi
     * złączeniami i grupowaniem (sprawdzone w logu SQL). Do policzenia ZAMÓWIEŃ wystarczy jedna tabela bez złączeń.
     */
    @Query(value = """
            select new s21_capstone.cap01_shop_api.order.OrderSummary(
                       o.id, c.name, o.status, o.total, o.createdAt, count(l))
            from ShopOrder o
                join o.customer c
                left join o.lines l
            where (:status is null or o.status = :status)
            group by o.id, c.name, o.status, o.total, o.createdAt""",
            countQuery = "select count(o) from ShopOrder o where (:status is null or o.status = :status)")
    Page<OrderSummary> findSummaries(@Param("status") OrderStatus status, Pageable pageable);

    /**
     * SZCZEGÓŁY jednego zamówienia: klient, pozycje i ich produkty w JEDNYM zapytaniu ({@code join fetch}).
     * Bez tego: 1 zapytanie o zamówienie + 1 o klienta + 1 o pozycje + po 1 na produkt każdej pozycji.
     */
    @Query("""
            select o from ShopOrder o
                join fetch o.customer
                left join fetch o.lines l
                left join fetch l.product
            where o.id = :id""")
    Optional<ShopOrder> findDetailedById(@Param("id") Long id);
}
