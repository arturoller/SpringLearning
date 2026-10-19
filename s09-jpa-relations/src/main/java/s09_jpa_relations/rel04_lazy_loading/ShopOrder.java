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
 * ShopOrder = zamówienie (tabela orders). OBIE relacje są leniwe (LAZY):
 * <ul>
 *   <li>{@code customer} — {@code @ManyToOne(fetch = FetchType.LAZY)}: LAZY trzeba wpisać, bo domyślnie *ToOne są EAGER;</li>
 *   <li>{@code lines} — {@code @OneToMany(mappedBy = "order")}: kolekcje są LAZY domyślnie.
 *       {@code @OrderBy("id")} (sortuj po) — Hibernate dopisze ORDER BY do zapytania, które wczytuje kolekcję.</li>
 * </ul>
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order")
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected ShopOrder() {
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getStatus() {
        return status;
    }

    public Customer getCustomer() {
        return customer;
    }

    /**
     * Celowo BEZ opakowania Collections.unmodifiableList — w tej lekcji oglądamy samą kolekcję Hibernate
     * (PersistentBag, Hibernate.isInitialized). Opakowanie ukrywa ją przed Hibernate.isInitialized/initialize
     * (sekcja 4 lekcji). Encja jest tylko do odczytu, więc nie potrzebuje metod addLine/removeLine.
     */
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

    @Override
    public String toString() {
        return "ShopOrder{id=" + id + ", number='" + number + "'}";   // bez customer i lines — LAZY!
    }
}
