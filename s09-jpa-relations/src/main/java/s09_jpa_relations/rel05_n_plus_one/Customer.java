package s09_jpa_relations.rel05_n_plus_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer = klient (tabela customers). Kolekcja {@code orders} ma {@code @BatchSize(size = 10)} (rozmiar paczki —
 * adnotacja Hibernate, nie JPA): gdy program sięgnie po zamówienia JEDNEGO klienta, Hibernate wczyta od razu
 * zamówienia do 10 klientów obecnych w kontekście utrwalania — jednym zapytaniem (sekcja 5 lekcji).
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String city;

    @OneToMany(mappedBy = "customer")
    @BatchSize(size = 10)
    private List<ShopOrder> orders = new ArrayList<>();

    protected Customer() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public List<ShopOrder> getOrders() {
        return orders;
    }

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

    @Override
    public String toString() {
        return "Customer{id=" + id + ", name='" + name + "'}";
    }
}
