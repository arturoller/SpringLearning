package s11_testing.test01_test_slices;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Product = produkt w sklepie. Encja JPA ({@code @Entity} — encja, obiekt zapisywany w tabeli). Cena w GROSZACH (int),
 * żeby uniknąć błędów zaokrągleń. Szczegóły encji: s08_data_jpa/Jpa01Entities.
 */
@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private int priceGrosze;

    /** Konstruktor bez argumentów jest wymagany przez JPA (Hibernate tworzy nim obiekty przy odczycie z bazy). */
    protected Product() {
    }

    public Product(String name, int priceGrosze) {
        this.name = name;
        this.priceGrosze = priceGrosze;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriceGrosze() {
        return priceGrosze;
    }
}
