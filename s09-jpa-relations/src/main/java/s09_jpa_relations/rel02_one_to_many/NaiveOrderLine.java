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

/** NaiveOrderLine = pozycja zamówienia „naiwnego” (tabela naive_order_lines; order_id MOŻE być NULL). */
@Entity
@Table(name = "naive_order_lines")
public class NaiveOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private NaiveOrder order;

    protected NaiveOrderLine() {
    }

    public NaiveOrderLine(String productName, int quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public NaiveOrder getOrder() {
        return order;
    }

    public void setOrder(NaiveOrder order) {
        this.order = order;
    }
}
