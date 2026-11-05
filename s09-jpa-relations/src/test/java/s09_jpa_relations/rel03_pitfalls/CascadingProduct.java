package s09_jpa_relations.rel03_pitfalls;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import s09_jpa_relations.rel03_many_to_many.Tag;

import java.util.HashSet;
import java.util.Set;

/**
 * ZŁA encja (PUŁAPKA sekcji 3 Rel03): {@code cascade = CascadeType.ALL} (w tym REMOVE) na {@code @ManyToMany}.
 * Usunięcie produktu każe Hibernate usunąć także jego TAGI — choć używają ich inne produkty.
 */
@Entity
@Table(name = "product")
public class CascadingProduct {

    @Id
    private Long id;

    private String name;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected CascadingProduct() {
    }

    public Set<Tag> getTags() {
        return tags;
    }
}
