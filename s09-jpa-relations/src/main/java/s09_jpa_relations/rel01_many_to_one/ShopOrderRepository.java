package s09_jpa_relations.rel01_many_to_one;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repozytorium zamówień lekcji Rel01ManyToOne — zapytania PO RELACJI (sekcja 4 lekcji). SQL każdej metody
 * (sprawdzony testem Rel01ManyToOneTest) jest w komentarzu nad nią.
 */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Metoda z nazwy przechodzi po relacji: Customer → Email. Spring Data robi z tego JOIN do tabeli customer:
    // select so1_0.id,so1_0.customer_id,so1_0.order_number,so1_0.ordered_on from orders so1_0
    //   join customer c1_0 on c1_0.id=so1_0.customer_id where c1_0.email=? order by so1_0.order_number
    List<ShopOrder> findByCustomerEmailOrderByOrderNumber(String email);

    // Warunek na ID klienta: Hibernate wie, że o.customer.id to po prostu kolumna customer_id w orders — BEZ joina:
    // select so1_0.id,so1_0.customer_id,so1_0.order_number,so1_0.ordered_on from orders so1_0
    //   where so1_0.customer_id=? order by so1_0.order_number
    @Query("select o from ShopOrder o where o.customer.id = :customerId order by o.orderNumber")
    List<ShopOrder> findOfCustomer(Long customerId);

    // To samo z nazwy metody: Customer → Id. Też bez joina (ten sam SQL co findOfCustomer — test).
    List<ShopOrder> findByCustomerIdOrderByOrderNumber(Long customerId);

    // Wszystkie zamówienia od razu z klientami — JEDNO zapytanie z JOIN (join fetch = złącz i pobierz).
    // Zapowiedź lekcji s09_jpa_relations/Rel05NPlusOne.
    @Query("select o from ShopOrder o join fetch o.customer order by o.orderNumber")
    List<ShopOrder> findAllWithCustomer();

    @Query("select o from ShopOrder o order by o.orderNumber")
    List<ShopOrder> findAllOrdered();
}
