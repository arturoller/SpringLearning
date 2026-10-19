package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ProductTag = tag (etykieta) produktu („wegańskie”, „promocja”, „bio”); tabela tags. Nazwa klasy to nie „Tag”, bo
 * tak nazywa się adnotacja JUnita {@code @Tag} używana w testach ćwiczeń. Strona ODWROTNA relacji wiele-do-wielu:
 * {@code @ManyToMany(mappedBy = "tags")} — tabelę łączącą product_tags opisuje pole tags w Product.
 */
@Entity
@Table(name = "tags")
public class ProductTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Product> products = new HashSet<>();

    protected ProductTag() {
    }

    public ProductTag(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Product> getProducts() {
        return Collections.unmodifiableSet(products);
    }

    /** Pakietowe — zmienia tylko Product.addTag/removeTag, które pilnują obu stron. */
    Set<Product> productsInternal() {
        return products;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductTag other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return ProductTag.class.hashCode();
    }

    @Override
    public String toString() {
        return "ProductTag{id=" + id + ", name='" + name + "'}";
    }
}
