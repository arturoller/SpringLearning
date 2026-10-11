package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * NaiveOrder = zamówienie „naiwne” — PRZED: brak metod pomocniczych, getter oddaje zmienną listę, kaskada tylko
 * PERSIST (utrwalanie), bez orphanRemoval. Służy do pokazania, że dopisanie do listy NIE ustawia klucza obcego.
 */
@Entity
@Table(name = "naive_orders")
public class NaiveOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<NaiveOrderLine> lines = new ArrayList<>();

    protected NaiveOrder() {
    }

    public NaiveOrder(String customerName) {
        this.customerName = customerName;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    /** Zmienna lista „na zewnątrz” — każdy może dopisać pozycję, zapominając o stronie właścicielskiej. */
    public List<NaiveOrderLine> getLines() {
        return lines;
    }
}
