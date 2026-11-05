package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repozytorium zamówień lekcji Rel04LazyLoading — sposoby na to, by leniwe relacje były wczytane, ZANIM zamknie się
 * transakcja (sekcja 3). SQL każdej metody (sprawdzony testem) jest w komentarzu nad nią.
 */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // join fetch = złącz i pobierz: klient, pozycje i ich produkty w JEDNYM zapytaniu:
    // select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,l1_0.order_id,l1_0.id,l1_0.product_id,
    //   p1_0.id,p1_0.name,p1_0.price,l1_0.quantity,l1_0.unit_price,so1_0.order_number from orders so1_0
    //   join customer c1_0 on c1_0.id=so1_0.customer_id join order_line l1_0 on so1_0.id=l1_0.order_id
    //   join product p1_0 on p1_0.id=l1_0.product_id where so1_0.id=? order by l1_0.id
    @Query("select o from ShopOrder o join fetch o.customer join fetch o.lines l join fetch l.product where o.id = :id")
    Optional<ShopOrder> findWithDetailsById(Long id);

    // entity graph = graf encji: lista atrybutów do wczytania OD RAZU, bez pisania JPQL. Zapytanie powstaje z nazwy
    // metody, a graf dokłada JOIN (zwykły, bo klient jest obowiązkowy — optional = false):
    // select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,so1_0.order_number from orders so1_0
    //   join customer c1_0 on c1_0.id=so1_0.customer_id where so1_0.order_number=?
    @EntityGraph(attributePaths = "customer")
    Optional<ShopOrder> findWithCustomerByOrderNumber(String orderNumber);

    // Projekcja do rekordu — żadnej encji, tylko dwie wartości (zwykły JOIN, nie fetch):
    // select so1_0.order_number,c1_0.name from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id
    //   where so1_0.order_number=?
    @Query("select new s09_jpa_relations.rel04_lazy_loading.OrderView(o.orderNumber, o.customer.name)"
            + " from ShopOrder o where o.orderNumber = :orderNumber")
    Optional<OrderView> findViewByOrderNumber(String orderNumber);
}
