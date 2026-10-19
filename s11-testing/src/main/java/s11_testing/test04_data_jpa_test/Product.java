package s11_testing.test04_data_jpa_test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

/**
 * Product = encja produktu (tabela {@code product} z migracji Flyway {@code db/migration/test04}). Cena w złotych
 * ({@code BigDecimal}). Ograniczenia (NOT NULL, UNIQUE, CHECK) są w BAZIE — encja ich celowo nie powtarza
 * (sekcja 3 lekcji: kiedy baza je sprawdza). Encje JPA od środka: dział s08_data_jpa.
 */
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // id nadaje baza (kolumna IDENTITY)
    private Long id;

    private String sku;
    private String name;
    private String category;
    private BigDecimal price;
    private int stock;

    /** Konstruktor bez argumentów — wymagany przez JPA (Hibernate tworzy nim obiekty przy odczycie). */
    protected Product() {
    }

    public Product(String sku, String name, String category, BigDecimal price, int stock) {
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

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
