package s09_jpa_relations.rel06_entity_to_dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Klient lekcji Rel06EntityToDto. Ma pole {@code passwordHash} (skrót hasła) — dane, które NIGDY nie mogą wyjść
 * poza aplikację. Encja ma do niego getter (potrzebny przy logowaniu), więc serializator JSON, który wypisuje
 * wszystkie gettery, wypisałby i jego (sekcja 1 lekcji). Na zewnątrz idzie {@link CustomerDto} bez tego pola.
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

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    protected Customer() { // dla Hibernate
    }

    public Customer(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
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

    public String getPasswordHash() {
        return passwordHash;
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

    /** Bez skrótu hasła — toString trafia do logów. */
    @Override
    public String toString() {
        return "Customer[id=" + id + ", name=" + name + "]";
    }
}
