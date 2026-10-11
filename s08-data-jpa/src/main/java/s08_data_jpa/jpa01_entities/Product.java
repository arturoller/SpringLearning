package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Product = produkt — encja (entity: klasa, której obiekty Hibernate zapisuje jako wiersze tabeli).
 *
 * <p>{@code @Entity} — „to jest encja”; {@code @Table(name = "products")} — nazwa tabeli (bez tej adnotacji byłoby
 * {@code product}); {@code @Id} — klucz główny; {@code @GeneratedValue(strategy = IDENTITY)} — wartość klucza nadaje
 * BAZA (kolumna z autonumeracją). {@code @Column} — szczegóły kolumny: nullable (czy może być NULL), length (długość
 * tekstu), unique (czy wartości muszą być unikalne), precision/scale (liczba cyfr ogółem / po przecinku).</p>
 *
 * <p>equals/hashCode: zgodnie z DOBRĄ PRAKTYKĄ z sekcji 6 lekcji — porównanie po id, ale tylko gdy id już jest,
 * i STAŁY hashCode (nie zmienia się po zapisie).</p>
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String sku;                       // SKU (stock keeping unit) = kod magazynowy produktu, np. "KAWA-001"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)              // enumerated = wyliczeniowy; STRING = zapisuj NAZWĘ stałej ("ACTIVE")
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    /** Konstruktor bez argumentów — WYMAGANY przez JPA (Hibernate tworzy nim obiekt przy odczycie z bazy). */
    protected Product() {
    }

    public Product(String name, String sku, BigDecimal price) {
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.status = ProductStatus.ACTIVE;
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

    public String getSku() {
        return sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product other)) {
            return false;
        }
        return id != null && id.equals(other.id);   // dwa NOWE (id == null) produkty nigdy nie są równe
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();               // stały — nie zmienia się, gdy baza nada id
    }

    @Override
    public String toString() {
        return "Product{id=" + id + ", name='" + name + "', sku='" + sku + "', price=" + price + ", status=" + status + "}";
    }
}
