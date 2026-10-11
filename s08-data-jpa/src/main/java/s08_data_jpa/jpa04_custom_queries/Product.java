package s08_data_jpa.jpa04_custom_queries;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Product = produkt — encja lekcji Jpa04CustomQueries. W JPQL piszemy nazwę ENCJI (Product) i nazwy PÓL (p.price),
 * w natywnym SQL — nazwę TABELI (products) i nazwy KOLUMN (price, stock).
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 30)
    private String category;                  // category = kategoria, np. "kawa", "herbata"

    @Column(nullable = false)
    private int stock;                        // stock = stan magazynowy (liczba sztuk)

    protected Product() {
    }

    public Product(String name, BigDecimal price, String category, int stock) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Product other && id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
