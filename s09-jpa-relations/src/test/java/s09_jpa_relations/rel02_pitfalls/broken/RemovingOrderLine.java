package s09_jpa_relations.rel02_pitfalls.broken;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import s09_jpa_relations.rel02_one_to_many.Product;

/**
 * ZŁA encja (PUŁAPKA sekcji 3 Rel02): {@code cascade = CascadeType.REMOVE} na {@code @ManyToOne}. Usunięcie POZYCJI
 * każe Hibernate usunąć także PRODUKT — „dziecko” zabija „rodzica”, którego używają inne pozycje.
 */
@Entity
@Table(name = "order_line")
public class RemovingOrderLine {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "product_id")
    private Product product;

    protected RemovingOrderLine() {
    }

    public Product getProduct() {
        return product;
    }
}
