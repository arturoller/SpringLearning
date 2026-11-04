package s09_jpa_relations.rel03_pitfalls;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import s09_jpa_relations.rel03_many_to_many.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * ZŁA encja (PUŁAPKA sekcji 2 Rel03): ten sam produkt i ta sama tabela łącząca co w lekcji, ale tagi jako
 * {@code List} — dla Hibernate „worek” (bag). Usunięcie JEDNEGO tagu kasuje i wstawia od nowa wszystkie wiersze.
 */
@Entity
@Table(name = "product")
public class BagProduct {

    @Id
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags = new ArrayList<>();

    protected BagProduct() {
    }

    public List<Tag> getTags() {
        return tags;
    }
}
