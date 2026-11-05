package s08_data_jpa.jpa06_flyway_with_jpa;

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
 * Encja produktu lekcji Jpa06FlywayWithJpa. Schemat tworzą migracje {@code db/migration/jpa06} (V1 tabela, V2 dane,
 * V3 kolumna weight_grams); Hibernate ({@code ddl-auto=validate}) przy starcie sprawdza, czy KAŻDE pole ma kolumnę
 * o zgodnym typie. Pole {@code weightGrams} dodano RAZEM z migracją V3 — bez V3 aplikacja nie wystartuje (sekcja 2).
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Dodane w V3. Nazwa kolumny weight_grams wynika z nazwy pola (strategia snake_case Springa). */
    private int weightGrams; // weight = waga, grams = gramy

    protected Product() { // dla Hibernate
    }

    public Product(String name, Category category, BigDecimal price, int stock, Instant createdAt, int weightGrams) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
        this.weightGrams = weightGrams;
    }

    public Long getId() {
        return id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getWeightGrams() {
        return weightGrams;
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
        return "Product[id=" + id + ", name=" + name + ", weightGrams=" + weightGrams + "]";
    }
}
