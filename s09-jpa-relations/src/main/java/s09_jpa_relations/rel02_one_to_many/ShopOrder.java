package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ShopOrder = zamówienie w sklepie (nie „Order”: to słowo zastrzeżone w SQL, a w Springu jest już klasa Sort.Order).
 * Tabela orders. Strona „jeden” relacji dwukierunkowej (bidirectional) i strona ODWROTNA (inverse side).
 *
 * <p>{@code @OneToMany(mappedBy = "order")} (jeden do wielu, „zmapowane przez pole order”) — „klucz obcy NIE jest
 * mój, opisuje go pole order w OrderLine”. {@code cascade = CascadeType.ALL} (kaskada: wszystkie operacje) — persist,
 * merge, remove... zamówienia przechodzą na pozycje. {@code orphanRemoval = true} (usuwanie sierot) — pozycja wyjęta
 * z listy jest usuwana z bazy.</p>
 *
 * <p>Getter zwraca listę NIEMODYFIKOWALNĄ — jedyna droga zmiany to addLine/removeLine, które pilnują OBU stron.</p>
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected ShopOrder() {
    }

    public ShopOrder(String customerName) {
        this.customerName = customerName;
    }

    /** Metoda pomocnicza: dodaje pozycję i ustawia stronę właścicielską (line.order). */
    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }

    /** Metoda pomocnicza: wyjmuje pozycję z listy i zeruje stronę właścicielską; orphanRemoval usunie wiersz. */
    public void removeLine(OrderLine line) {
        lines.remove(line);
        line.setOrder(null);
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShopOrder other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return ShopOrder.class.hashCode();
    }

    @Override
    public String toString() {
        return "ShopOrder{id=" + id + ", customerName='" + customerName + "'}";   // bez lines — LAZY!
    }
}
