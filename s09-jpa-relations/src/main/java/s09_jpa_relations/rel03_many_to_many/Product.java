package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Produkt — STRONA WŁAŚCICIELA relacji produkt N—N tag: to pole {@code tags} z {@code @JoinTable} decyduje, jakie
 * wiersze są w tabeli łączącej {@code product_tag}. Kolekcja to {@code Set} — dlaczego nie {@code List}: sekcja 2.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // many to many = wiele do wielu; join table = tabela łącząca;
    // joinColumns = kolumna wskazująca TĘ encję (product_id), inverseJoinColumns = kolumna wskazująca drugą (tag_id).
    // Bez kaskady (cascade) — tagi żyją niezależnie od produktów (sekcja 3). Domyślnie LAZY.
    @ManyToMany
    @JoinTable(name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected Product() { // dla Hibernate
    }

    public Product(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    /** Metoda pomocnicza: ustawia OBIE strony (zbiór tagów produktu i zbiór produktów tagu) — jak addLine w Rel02. */
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getProductsInternal().add(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getProductsInternal().remove(this);
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

    /** Widok tylko do odczytu — zmiany wyłącznie przez addTag/removeTag (obie strony naraz). */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /** equals po id z obsługą null, stały hashCode — WARUNEK poprawnego działania Set (s08_data_jpa/Jpa01Entities). */
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
        return "Product[id=" + id + ", name=" + name + ", price=" + price + "]";
    }
}
