package s09_jpa_relations.rel06_entity_to_dto;

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
 * Customer = klient (tabela customers). Strona „jeden” relacji DWUKIERUNKOWEJ z ShopOrder: klient zna swoje
 * zamówienia ({@code orders}, LAZY), a zamówienie zna klienta. Pole {@code internalNote} (notatka wewnętrzna)
 * jest tylko dla pracowników sklepu.
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

    @Column(name = "internal_note", length = 200)
    private String internalNote;

    @OneToMany(mappedBy = "customer")
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

    public String getEmail() {
        return email;
    }

    public String getInternalNote() {
        return internalNote;
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
