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

import java.util.ArrayList;
import java.util.List;

/**
 * ListProduct = produkt z tagami w LIŚCIE (PRZED). Relacja jednokierunkowa, tabela łącząca list_product_tags.
 * Lista bez @OrderColumn to dla Hibernate „worek” (bag) — patrz sekcja 2 lekcji: usunięcie jednego tagu kosztuje
 * DELETE wszystkich wierszy produktu i ponowne INSERT-y pozostałych.
 */
@Entity
@Table(name = "list_products")
public class ListProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany
    @JoinTable(name = "list_product_tags",
            joinColumns = @JoinColumn(name = "list_product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<ProductTag> tags = new ArrayList<>();

    protected ListProduct() {
    }

    public ListProduct(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public List<ProductTag> getTags() {
        return tags;
    }
}
