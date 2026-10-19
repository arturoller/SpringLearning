package s08_data_jpa.jpa05_paging_sorting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Produkt sklepu (lekcja Jpa05PagingSorting). W Sort.by("price") i PageRequest podajesz nazwy PÓL tej klasy
 * (sku, name, category, price, stock), nie nazwy kolumn. Uwaga na remisy: KSI-002 i DOM-002 kosztują po 129.00,
 * a wiele produktów ma tę samą kategorię — sortowanie tylko po takim polu nie wyznacza kolejności (sekcja 4).
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private int stock;

    protected Product() {
    }

    public Product(String sku, String name, Category category, BigDecimal price, int stock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
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

    public Category getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    /** Równość po kluczu biznesowym sku (uzasadnienie: lekcja Jpa01Entities, sekcja 7). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return Objects.equals(sku, other.getSku());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sku);
    }

    @Override
    public String toString() {
        return sku;
    }
}
