package s08_data_jpa.jpa01_pitfalls;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Objects;

/**
 * Pułapka c) z sekcji 5 lekcji Jpa01Entities: equals/hashCode na WSZYSTKICH polach (tak generuje Lombok @Data
 * albo IDE „Generate equals() and hashCode()”). Nie rób tak w encjach.
 */
@Entity
public class AllFieldsEqualsProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected AllFieldsEqualsProduct() {
    }

    public AllFieldsEqualsProduct(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AllFieldsEqualsProduct other && Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name); // zmienia się, gdy persist nada id albo zmieni się nazwa
    }
}
