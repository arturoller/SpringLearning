package s09_jpa_relations.rel02_pitfalls.jointable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

/** Paczka (parcel) z jednokierunkowym {@code @OneToMany} BEZ mappedBy i BEZ @JoinColumn → Hibernate chce tabeli łączącej. */
@Entity
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ParcelItem> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public List<ParcelItem> getItems() {
        return items;
    }
}
