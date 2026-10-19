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
 * Product = produkt. Strona WŁAŚCICIELSKA relacji wiele-do-wielu z ProductTag: to pole tags opisuje tabelę łączącą.
 *
 * <p>{@code @ManyToMany} (wiele do wielu) + {@code @JoinTable} (tabela łącząca): name — nazwa tabeli,
 * joinColumns — kolumna wskazująca TĘ encję (product_id), inverseJoinColumns — kolumna wskazująca drugą stronę
 * (tag_id). Kolekcja to Set (zbiór) — patrz sekcja 2 lekcji. Bez kaskad: tagi żyją własnym życiem.</p>
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

    @ManyToMany
    @JoinTable(name = "product_tags",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<ProductTag> tags = new HashSet<>();

    protected Product() {
    }

    public Product(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    /** Metoda pomocnicza: dodaje tag po obu stronach relacji. */
    public void addTag(ProductTag tag) {
        tags.add(tag);
        tag.productsInternal().add(this);
    }

    /** Metoda pomocnicza: usuwa tag po obu stronach relacji (wiersz z product_tags zniknie przy flush). */
    public void removeTag(ProductTag tag) {
        tags.remove(tag);
        tag.productsInternal().remove(this);
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

    public Set<ProductTag> getTags() {
        return Collections.unmodifiableSet(tags);
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
        return "Product{id=" + id + ", name='" + name + "'}";
    }
}
