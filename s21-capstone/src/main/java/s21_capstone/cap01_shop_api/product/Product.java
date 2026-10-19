package s21_capstone.cap01_shop_api.product;

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
 * Encja produktu (entity = encja — klasa odwzorowana na tabelę {@code products}).
 *
 * <p>Techniczne {@code id} (klucz z bazy) i biznesowe {@code sku} (numer katalogowy, unikalny). API pokazuje TYLKO sku —
 * id to szczegół bazy. Pola zmieniamy metodami z nazwą biznesową ({@link #update}), a nie seterami do każdego pola:
 * encja sama pilnuje swoich reguł (cena dodatnia, stan nieujemny).</p>
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // identity = numer nadaje baza przy INSERT
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)   // STRING: w kolumnie "KSIAZKI", a nie numer pozycji (który zmienia się po dopisaniu stałej)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    /** Konstruktor dla Hibernate (JPA wymaga bezargumentowego; protected — nie dla naszego kodu). */
    protected Product() {
    }

    public Product(String sku, String name, Category category, BigDecimal price, int stock) {
        this.sku = sku;
        update(name, category, price, stock);
    }

    /** Zmiana danych produktu (PUT) — z regułami, których pilnuje sama encja. */
    public void update(String name, Category category, BigDecimal price, int stock) {
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("Cena musi być dodatnia: " + price);
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stan nie może być ujemny: " + stock);
        }
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    /**
     * Zdjęcie towaru z magazynu (używa go ĆWICZENIE 3 — blokada zamówienia przy braku towaru).
     *
     * @throws IllegalStateException gdy towaru jest za mało
     */
    public void removeFromStock(int quantity) {
        if (quantity > stock) {
            throw new IllegalStateException("Za mało towaru " + sku + ": jest " + stock + ", potrzeba " + quantity);
        }
        stock -= quantity;
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
}
