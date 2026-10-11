package s08_data_jpa.jpa01_entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Category = kategoria produktów. Klucz z SEKWENCJI ({@code GenerationType.SEQUENCE}): sequence (sekwencja) to obiekt
 * w bazie, który wydaje kolejne liczby. Hibernate pobiera numer z sekwencji JUŻ przy persist, a sam INSERT odkłada
 * do flush (wypchnięcia zmian) — porównaj z Product (IDENTITY) w sekcji 2 lekcji.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
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

    public void setName(String name) {
        this.name = name;
    }
}
