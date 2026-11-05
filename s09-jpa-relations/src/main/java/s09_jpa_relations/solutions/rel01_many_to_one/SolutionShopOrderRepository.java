package s09_jpa_relations.solutions.rel01_many_to_one;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s09_jpa_relations.rel01_many_to_one.CustomerOrderCount;
import s09_jpa_relations.rel01_many_to_one.ShopOrder;

import java.util.List;

/** Zapytania do rozwiązań wzorcowych lekcji Rel01ManyToOne (odpowiednik ExerciseShopOrderRepository). */
public interface SolutionShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Rozwiązanie 1: przejście po relacji w nazwie metody → JOIN customer, warunek c1_0.name=?
    List<ShopOrder> findByCustomerNameOrderByOrderNumber(String customerName);

    // Rozwiązanie 3: count z JOIN-em do customer (e-mail jest w tabeli klienta)
    long countByCustomerEmail(String email);

    // Rozwiązanie 4: agregat z grupowaniem; rekord bez "new" — Spring Data dopasowuje kolumny do konstruktora
    @Query("select o.customer.name, count(o) from ShopOrder o group by o.customer.name order by o.customer.name")
    List<CustomerOrderCount> countOrdersPerCustomer();
}
