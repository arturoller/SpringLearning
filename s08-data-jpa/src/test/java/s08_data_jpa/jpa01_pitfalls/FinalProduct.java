package s08_data_jpa.jpa01_pitfalls;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Pułapka b) z sekcji 5 lekcji Jpa01Entities: klasa encji {@code final} — Hibernate nie zrobi z niej proxy. */
@Entity
public final class FinalProduct {

    @Id
    private Long id;

    private String name;

    protected FinalProduct() {
    }

    public FinalProduct(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
