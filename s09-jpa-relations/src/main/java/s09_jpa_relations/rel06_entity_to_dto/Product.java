package s09_jpa_relations.rel06_entity_to_dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Product = produkt (tabela products) — encja BEZ relacji. Pole {@code purchasePrice} (cena zakupu u dostawcy)
 * to tajemnica handlowa sklepu: kto ją zna, zna naszą marżę. Sekcja 1 lekcji pokazuje, że zwrócenie encji
 * z kontrolera wysyła je do każdego klienta API.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    protected Product() {
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Product.class.hashCode();
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", sku='" + sku + "'}";
    }
}
