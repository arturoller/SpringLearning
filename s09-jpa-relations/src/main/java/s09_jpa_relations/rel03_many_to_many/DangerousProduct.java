package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * DangerousProduct = produkt „niebezpieczny” — PUŁAPKA: {@code @ManyToMany(cascade = CascadeType.REMOVE)}.
 * Usunięcie produktu próbuje usunąć też jego TAGI — a tagi należą również do innych produktów (sekcja 4 lekcji).
 */
@Entity
@Table(name = "dangerous_products")
public class DangerousProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(cascade = CascadeType.REMOVE)          // NIE RÓB TEGO — tylko do pokazania skutków
    @JoinTable(name = "dangerous_product_tags",
            joinColumns = @JoinColumn(name = "dangerous_product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected DangerousProduct() {
    }

    public DangerousProduct(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Set<Tag> getTags() {
        return tags;
    }
}
