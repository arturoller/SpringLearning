package s09_jpa_relations.rel05_n_plus_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Zamówienie lekcji Rel05NPlusOne: leniwy klient, leniwa lista pozycji i leniwa lista płatności.
 * {@code @NamedEntityGraph} (nazwany graf encji, sekcja 3) i {@code @BatchSize} na płatnościach (sekcja 4) to dwa
 * sposoby na to, by NIE dociągać relacji osobnym zapytaniem dla każdego zamówienia.
 */
@Entity
@Table(name = "orders")
// named entity graph = nazwany graf encji: zestaw relacji do wczytania, używany z repozytorium przez nazwę
@NamedEntityGraph(name = "ShopOrder.withCustomerAndLines",
        attributeNodes = {@NamedAttributeNode("customer"), @NamedAttributeNode("lines")})
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order")
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    // batch size = rozmiar paczki (adnotacja Hibernate, nie JPA: org.hibernate.annotations): gdy trzeba wczytać
    // płatności JEDNEGO zamówienia, Hibernate wczyta od razu płatności do 5 zamówień z kontekstu — jednym IN (…)
    @OneToMany(mappedBy = "order")
    @OrderBy("id")
    @BatchSize(size = 5)
    private List<Payment> payments = new ArrayList<>();

    protected ShopOrder() { // dla Hibernate
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

    public List<Payment> getPayments() {
        return payments;
    }

    /** Wartość zamówienia z pozycji (wczytuje pozycje, jeśli jeszcze nie są wczytane). */
    public BigDecimal total() {
        return lines.stream()
                .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    @Override
    public String toString() {
        return "ShopOrder[id=" + id + ", orderNumber=" + orderNumber + "]";
    }
}
