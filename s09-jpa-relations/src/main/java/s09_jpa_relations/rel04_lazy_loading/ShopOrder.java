package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Zamówienie lekcji Rel04LazyLoading — OBIE relacje leniwe: klient ({@code @ManyToOne(fetch = LAZY)} — jawnie, bo
 * domyślne jest EAGER) i lista pozycji ({@code @OneToMany} — LAZY domyślnie). Po {@code find} w polu {@code customer}
 * siedzi proxy {@code Customer$HibernateProxy…}, a w polu {@code lines} — niezainicjalizowany {@code PersistentBag}.
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    // fetch = LAZY — leniwie (proxy zamiast od razu wczytanego klienta); optional = false — zamówienie MUSI mieć klienta
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order")
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected ShopOrder() { // dla Hibernate
    }

    public ShopOrder(String orderNumber, Customer customer) {
        this.orderNumber = orderNumber;
        this.customer = customer;
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

    public List<OrderLine> getLines() {
        return lines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShopOrder other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return ShopOrder.class.hashCode();
    }

    /** Bez klienta i pozycji: toString nie może inicjalizować leniwych relacji (Rel01, sekcja 2 — PUŁAPKA). */
    @Override
    public String toString() {
        return "ShopOrder[id=" + id + ", orderNumber=" + orderNumber + "]";
    }
}
