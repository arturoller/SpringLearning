package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * DRUGA encja na tej samej tabeli {@code orders} — tylko do porównania w sekcji 2 lekcji. Różnica: {@code @ManyToOne}
 * BEZ {@code fetch}, czyli z domyślnym (wg specyfikacji JPA) {@code FetchType.EAGER} (eager = zachłannie, od razu).
 * Hibernate pozwala mapować jedną tabelę dwiema encjami; w prawdziwej aplikacji tak nie robimy — tu to „szkiełko”
 * do porównania SQL. Nazwa encji w JPQL: {@code EagerShopOrder}. Tylko do odczytu (bez konstruktora publicznego).
 */
@Entity
@Table(name = "orders")
public class EagerShopOrder {

    @Id
    private Long id;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    @ManyToOne // = @ManyToOne(fetch = FetchType.EAGER) — domyślne wg JPA dla relacji "do jednego"
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    protected EagerShopOrder() { // dla Hibernate
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }
}
