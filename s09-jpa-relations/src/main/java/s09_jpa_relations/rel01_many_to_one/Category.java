package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Category = kategoria — strona „jeden” relacji. Zwróć uwagę: kategoria NIE ma tu listy produktów. Relacja jest
 * jednokierunkowa (unidirectional): produkt zna swoją kategorię, kategoria nie zna produktów. To w zupełności
 * wystarcza w wielu przypadkach — listę produktów kategorii daje zapytanie (ProductRepository.findByCategoryName).
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    protected Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category other)) {
            return false;
        }
        // getId(), a NIE other.id: „other” może być proxy (zastępcą) Hibernate, którego pola są puste.
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Category.class.hashCode();   // stały; Category.class, bo getClass() proxy to inna (pod)klasa
    }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}
