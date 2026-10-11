package s09_jpa_relations.rel03_many_to_many;

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
 * OrderLine = pozycja zamówienia — relacja zamówienie ↔ produkt z DODATKOWĄ kolumną quantity (ilość).
 * Zamiast {@code @ManyToMany} — osobna encja z dwiema relacjami {@code @ManyToOne} i własnym id.
 */
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private ShopOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    protected OrderLine() {
    }

    public OrderLine(ShopOrder order, Product product, int quantity) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public ShopOrder getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}
