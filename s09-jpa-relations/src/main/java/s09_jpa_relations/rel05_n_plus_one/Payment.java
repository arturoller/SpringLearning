package s09_jpa_relations.rel05_n_plus_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Płatność (payment) za zamówienie — zamówienie można opłacić w kilku ratach. To DRUGA kolekcja zamówienia
 * (obok pozycji): sekcja 4 lekcji pokazuje, co się dzieje przy {@code join fetch} dwóch list naraz.
 */
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    protected Payment() { // dla Hibernate
    }

    public Long getId() {
        return id;
    }

    public ShopOrder getOrder() {
        return order;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Payment.class.hashCode();
    }

    @Override
    public String toString() {
        return "Payment[id=" + id + ", amount=" + amount + "]";
    }
}
