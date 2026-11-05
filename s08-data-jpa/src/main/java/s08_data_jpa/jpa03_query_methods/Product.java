package s08_data_jpa.jpa03_query_methods;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Encja produktu lekcji Jpa03QueryMethods (mapowanie jak w s08_data_jpa/Jpa01Entities). Tabelę i dziesięć produktów
 * tworzą migracje Flyway {@code db/migration/jpa03}; Hibernate ({@code ddl-auto=validate}) tylko sprawdza schemat.
 * Nazwy PÓL (name, category, price, stock, supplier) to słowa, z których budujesz nazwy metod zapytań.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private int stock;

    @Column(length = 100) // może być NULL — nie każdy produkt ma dostawcę (sekcja 4)
    private String supplier; // supplier = dostawca

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Product() { // dla Hibernate
    }

    public Product(String name, Category category, BigDecimal price, int stock, String supplier, Instant createdAt) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.supplier = supplier;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getSupplier() {
        return supplier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** equals po id z obsługą null, stały hashCode — dlaczego tak: s08_data_jpa/Jpa01Entities, sekcja 5. */
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
        return "Product[id=" + id + ", name=" + name + ", price=" + price + ", stock=" + stock + "]";
    }
}
