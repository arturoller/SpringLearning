package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Product = produkt — strona „wiele” relacji i jednocześnie strona WŁAŚCICIELSKA (owning side): w JEJ tabeli
 * (products) leży klucz obcy category_id.
 *
 * <p>{@code @ManyToOne} (wiele do jednego) — wiele produktów należy do jednej kategorii.
 * {@code fetch = FetchType.LAZY} (pobieranie leniwe) — kategoria NIE jest czytana razem z produktem; Hibernate wstawia
 * w pole proxy (zastępcę), które dociągnie dane przy pierwszym użyciu. {@code optional = false} — produkt MUSI mieć
 * kategorię (Hibernate sprawdza to przed INSERT). {@code @JoinColumn(name = "category_id", nullable = false)}
 * (kolumna złączenia) — nazwa kolumny klucza obcego i informacja, że nie może być NULL.</p>
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected Product() {
    }

    public Product(String name, BigDecimal price, Category category) {
        this.name = name;
        this.price = price;
        this.category = category;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
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
        // Bez category: toString nie powinien uruchamiać leniwego ładowania (dodatkowy SELECT albo wyjątek).
        return "Product{id=" + id + ", name='" + name + "', price=" + price + "}";
    }
}
