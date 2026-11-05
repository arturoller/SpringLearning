package s09_jpa_relations.rel02_pitfalls.jointable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

/**
 * Paleta (pallet) z jednokierunkowym {@code @OneToMany} i {@code @JoinColumn(nullable = false)} — porównanie z Invoice
 * lekcji (tam invoice_id dopuszcza NULL): czy Hibernate nadal robi INSERT + UPDATE?
 */
@Entity
public class Pallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "pallet_id", nullable = false)
    private List<PalletItem> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public List<PalletItem> getItems() {
        return items;
    }
}
