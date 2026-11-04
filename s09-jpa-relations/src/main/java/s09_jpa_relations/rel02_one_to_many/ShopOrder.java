package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Zamówienie — strona „jeden” relacji DWUKIERUNKOWEJ (bidirectional) zamówienie 1—N pozycja. Tabela {@code orders}
 * nie ma kolumny o pozycjach: lista {@code lines} to „lustro” klucza obcego {@code order_line.order_id}, którym
 * rządzi pole {@code OrderLine.order} (strona właściciela). {@code mappedBy = "order"} mówi Hibernate właśnie to.
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    // one to many = jeden do wielu; mapped by = zmapowane przez (pole "order" w OrderLine — tam jest klucz obcy);
    // cascade = ALL — persist/merge/remove/refresh/detach zamówienia przechodzą na pozycje (sekcja 3);
    // orphan removal = usuwanie sierot — pozycja wyjęta z listy jest usuwana z bazy (sekcja 3);
    // order by = sortuj według — kolejność elementów przy wczytaniu listy (sekcja 4). Domyślnie LAZY.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected ShopOrder() { // dla Hibernate
    }

    public ShopOrder(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    /**
     * DOBRA PRAKTYKA: (sekcja 1) metoda pomocnicza ustawia OBIE strony relacji naraz — pozycja dostaje zamówienie
     * (to trafi do order_id), a lista zamówienia dostaje pozycję (spójność obiektów w pamięci).
     */
    public OrderLine addLine(Product product, int quantity) {
        OrderLine line = new OrderLine(product, quantity, product.getPrice());
        addLine(line);
        return line;
    }

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }

    /** Odwrotność addLine: zdejmuje pozycję z listy i zeruje jej zamówienie; z orphanRemoval = DELETE przy flush. */
    public void removeLine(OrderLine line) {
        lines.remove(line);
        line.setOrder(null);
    }

    /**
     * Wartość zamówienia policzona w Javie z pozycji (sekcja 5). Wygodne, gdy pozycje i tak są wczytane;
     * dla samej liczby lepsze jest zapytanie agregujące (OrderLineRepository.sumTotalOfOrder).
     */
    public BigDecimal total() {
        return lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    /**
     * Zwraca ŻYWĄ listę (nie kopię) — dzięki temu w sekcji 1 widać PUŁAPKĘ {@code getLines().add(…)} bez
     * {@code setOrder}. W kodzie produkcyjnym częściej zwraca się widok tylko do odczytu
     * ({@code Collections.unmodifiableList(lines)}), żeby jedyną drogą zmiany były addLine/removeLine.
     */
    public List<OrderLine> getLines() {
        return lines;
    }

    /** ZŁY POMYSŁ — istnieje tylko dla PUŁAPKI z sekcji 4 (podmiana kolekcji przy orphanRemoval). */
    public void setLines(List<OrderLine> lines) {
        this.lines = lines;
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

    /** Bez listy pozycji: wypisanie jej wczytałoby pozycje z bazy, a przy toString pozycji z zamówieniem — pętla. */
    @Override
    public String toString() {
        return "ShopOrder[id=" + id + ", orderNumber=" + orderNumber + "]";
    }
}
