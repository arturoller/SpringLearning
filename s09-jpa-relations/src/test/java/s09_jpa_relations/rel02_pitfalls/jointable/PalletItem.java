package s09_jpa_relations.rel02_pitfalls.jointable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Element palety — bez pola z paletą. */
@Entity
public class PalletItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected PalletItem() {
    }

    public PalletItem(String name) {
        this.name = name;
    }
}
