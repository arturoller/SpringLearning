package s11_testing.test04_data_jpa_test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Encja kategorii. Kolekcja {@code products} jest LENIWA (lazy — dane pobierane dopiero przy pierwszym użyciu):
 * poza transakcją jej odczyt kończy się {@code LazyInitializationException} (s09_jpa_relations/Rel04LazyLoading).
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @OneToMany(mappedBy = "category") // domyślnie fetch = LAZY dla kolekcji
    private List<Product> products = new ArrayList<>();

    protected Category() { // dla Hibernate
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

    public List<Product> getProducts() {
        return products;
    }
}
