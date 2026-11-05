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

import java.math.BigDecimal;

/**
 * Pozycja zamówienia — strona „wiele” i STRONA WŁAŚCICIELA (owning side) relacji z zamówieniem: to pole
 * {@code order} decyduje, co Hibernate wpisze do kolumny {@code order_line.order_id}. Druga relacja
 * ({@code product}) to zwykłe {@code @ManyToOne} jak w Rel01.
 */
@Entity
@Table(name = "order_line")
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

    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected OrderLine() { // dla Hibernate
    }

    public OrderLine(Product product, int quantity, BigDecimal unitPrice) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /** Wartość pozycji = ilość × cena jednostkowa. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    public ShopOrder getOrder() {
        return order;
    }

    /**
     * Publiczny, bo w sekcji 1 pokazujemy też ustawianie TYLKO strony właściciela. Na co dzień woła go
     * {@code ShopOrder.addLine/removeLine} — tam obie strony zmieniają się razem.
     */
    public void setOrder(ShopOrder order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
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

    /** Bez pól order i product (relacje) — patrz PUŁAPKA toString w sekcji 1 lekcji. */
    @Override
    public String toString() {
        return "OrderLine[id=" + id + ", quantity=" + quantity + ", unitPrice=" + unitPrice + "]";
    }
}
