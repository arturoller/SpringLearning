package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * EagerProduct = produkt „zachłanny” — PRZED: {@code @ManyToOne} BEZ podanego fetch. Domyślnie (według specyfikacji JPA)
 * relacje *ToOne są EAGER (zachłanne): każdy odczyt produktu od razu czyta też kategorię. Tabela eager_products.
 */
@Entity
@Table(name = "eager_products")
public class EagerProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne                               // brak fetch → EAGER; brak optional → relacja opcjonalna
    @JoinColumn(name = "category_id")
    private Category category;

    protected EagerProduct() {
    }

    public EagerProduct(String name, Category category) {
        this.name = name;
        this.category = category;
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
}
