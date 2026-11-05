package s08_data_jpa.jpa01_pitfalls;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

/**
 * Poprawna (nie-final) encja do porównania z FinalProduct. Pole price celowo BEZ {@code @Column(precision, scale)} —
 * test pokazuje, jaki typ kolumny wybiera wtedy Hibernate.
 */
@Entity
public class OpenProduct {

    @Id
    private Long id;

    private String name;

    private BigDecimal price;

    protected OpenProduct() {
    }

    public OpenProduct(Long id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }
}
