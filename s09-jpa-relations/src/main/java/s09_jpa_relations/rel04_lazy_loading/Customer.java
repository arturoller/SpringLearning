package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Customer = klient (tabela customers). Zwykła encja bez relacji — w tej lekcji pojawia się głównie jako
 * PROXY (zastępca) w polu {@code ShopOrder.customer}.
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

    @Column(length = 100)
    private String email;

    protected Customer() {
    }

    public Customer(String name, String city, String email) {
        this.name = name;
        this.city = city;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer other)) {
            return false;
        }
        return id != null && id.equals(other.getId());   // getId(), bo „other” może być proxy
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
