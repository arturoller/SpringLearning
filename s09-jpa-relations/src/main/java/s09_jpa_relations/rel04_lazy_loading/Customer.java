package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Klient lekcji Rel04LazyLoading. Ma ZBIÓR (Set) zamówień po stronie odwrotnej relacji ({@code mappedBy}) — żeby
 * pokazać drugą kolekcję Hibernate: {@code PersistentSet} (lista zamówienia to {@code PersistentBag}).
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    // one to many = jeden do wielu; domyślnie LAZY — zamówienia wczytają się dopiero przy pierwszym użyciu zbioru
    @OneToMany(mappedBy = "customer")
    private Set<ShopOrder> orders = new LinkedHashSet<>();

    protected Customer() { // dla Hibernate
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Set<ShopOrder> getOrders() {
        return orders;
    }

    /** equals po id z obsługą null, stały hashCode — dlaczego tak: s08_data_jpa/Jpa01Entities, sekcja 5. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Customer.class.hashCode();
    }

    /** Bez zbioru zamówień — wypisanie go wczytałoby zamówienia (a poza transakcją rzuciło wyjątek). */
    @Override
    public String toString() {
        return "Customer[id=" + id + ", name=" + name + "]";
    }
}
