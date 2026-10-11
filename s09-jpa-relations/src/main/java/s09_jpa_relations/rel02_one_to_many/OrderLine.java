package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * OrderLine = pozycja zamówienia (tabela order_lines). Strona „wiele” i strona WŁAŚCICIELSKA: pole order z
 * {@code @JoinColumn(name = "order_id")} decyduje o wartości klucza obcego.
 *
 * <p>setOrder jest pakietowy (bez public) — z zewnątrz pozycję podpina się przez ShopOrder.addLine.</p>
 */
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    protected OrderLine() {
    }

    public OrderLine(String productName, int quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public ShopOrder getOrder() {
        return order;
    }

    void setOrder(ShopOrder order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderLine other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return OrderLine.class.hashCode();
    }

    @Override
    public String toString() {
        return "OrderLine{id=" + id + ", productName='" + productName + "', quantity=" + quantity + "}";
    }
}
