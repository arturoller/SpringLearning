package s08_data_jpa.jpa02_repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Product = produkt z kluczem NADAWANYM RĘCZNIE (assigned id): kodem SKU. Bez @GeneratedValue id jest ustawione już
 * w konstruktorze, więc Spring Data uznaje KAŻDY taki obiekt za „nie nowy” i w save woła merge — z dodatkowym
 * SELECT przed INSERT. PUŁAPKA z sekcji 3 lekcji Jpa02Repositories.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 20)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal price) {
        this.sku = sku;
        this.name = name;
        this.price = price;
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

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Product other && sku != null && sku.equals(other.sku);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
