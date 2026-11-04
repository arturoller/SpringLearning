package s09_jpa_relations.rel02_pitfalls.jointable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Element paczki — bez pola z paczką (relacja jednokierunkowa od strony Parcel). */
@Entity
public class ParcelItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected ParcelItem() {
    }

    public ParcelItem(String name) {
        this.name = name;
    }
}
