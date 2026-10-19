package s21_capstone.cap01_shop_api.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import s21_capstone.cap01_shop_api.product.Product;

import java.math.BigDecimal;

/**
 * Pozycja zamówienia: produkt, ilość i cena JEDNOSTKOWA Z CHWILI ZAMÓWIENIA. Cena jest kopiowana z produktu,
 * a nie czytana z niego przy każdym wyświetleniu — gdy sklep jutro podniesie cenę laptopa, wczorajsze zamówienie
 * ma nadal tę samą sumę (tak działa każdy paragon).
 *
 * <p>Pozycję tworzy wyłącznie {@link ShopOrder#addLine} (konstruktor pakietowy) — zamówienie pilnuje swojej sumy.</p>
 */
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // LAZY — domyślne EAGER dla ToOne to częsta przyczyna N+1
    @JoinColumn(name = "order_id")
    private ShopOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected OrderLine() {
    }

    OrderLine(ShopOrder order, Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Ilość musi być dodatnia: " + quantity);
        }
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();   // KOPIA ceny z tej chwili
    }

    /** Wartość pozycji = cena jednostkowa × ilość. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
