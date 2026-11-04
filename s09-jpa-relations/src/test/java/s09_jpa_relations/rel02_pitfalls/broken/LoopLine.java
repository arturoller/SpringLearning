package s09_jpa_relations.rel02_pitfalls.broken;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** ZŁA encja (PUŁAPKA sekcji 1 Rel02): toString wypisuje zamówienie, które wypisuje swoje pozycje… */
@Entity
@Table(name = "order_line")
public class LoopLine {

    @Id
    private Long id;

    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private LoopOrder order;

    protected LoopLine() {
    }

    @Override
    public String toString() {
        return "LoopLine[id=" + id + ", quantity=" + quantity + ", order=" + order + "]";
    }
}
