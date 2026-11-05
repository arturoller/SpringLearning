package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Pozycja faktury (sekcja 2 lekcji) — celowo BEZ pola z fakturą: relacja jest jednokierunkowa od strony Invoice. */
@Entity
@Table(name = "invoice_line")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    protected InvoiceLine() { // dla Hibernate
    }

    public InvoiceLine(String description, BigDecimal amount) {
        this.description = description;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvoiceLine other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return InvoiceLine.class.hashCode();
    }

    @Override
    public String toString() {
        return "InvoiceLine[id=" + id + ", description=" + description + ", amount=" + amount + "]";
    }
}
