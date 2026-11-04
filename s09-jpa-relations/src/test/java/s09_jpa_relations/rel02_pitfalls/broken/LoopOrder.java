package s09_jpa_relations.rel02_pitfalls.broken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/** ZŁA encja (PUŁAPKA sekcji 1 Rel02): toString wypisuje listę pozycji, a pozycja wypisuje zamówienie → pętla. */
@Entity
@Table(name = "orders")
public class LoopOrder {

    @Id
    private Long id;

    @Column(name = "order_number") // ta sama nazwa logiczna kolumny co w ShopOrder lekcji (ta sama tabela!)
    private String orderNumber;

    @OneToMany(mappedBy = "order")
    private List<LoopLine> lines = new ArrayList<>();

    protected LoopOrder() {
    }

    public List<LoopLine> getLines() {
        return lines;
    }

    // Tak wygląda toString wygenerowany przez IDE „ze wszystkich pól” albo przez Lombok @ToString/@Data.
    @Override
    public String toString() {
        return "LoopOrder[id=" + id + ", orderNumber=" + orderNumber + ", lines=" + lines + "]";
    }
}
