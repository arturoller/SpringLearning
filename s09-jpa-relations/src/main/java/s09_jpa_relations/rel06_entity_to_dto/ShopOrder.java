package s09_jpa_relations.rel06_entity_to_dto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Zamówienie lekcji Rel06EntityToDto. Encja pilnuje swoich reguł: nowe zamówienie ma status NEW, pozycje dodaje
 * się tylko przez {@link #addLine}, a lista na zewnątrz jest tylko do odczytu. Nie ma setterów do id, statusu ani
 * listy — dzięki temu nikt (także „sprytne” mapowanie z żądania) nie ustawi ich z zewnątrz (sekcja 1: mass assignment).
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING) // enumerated = wyliczeniowy; STRING = zapis nazwy stałej, nie numeru
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(length = 200)
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderLine> lines = new ArrayList<>();

    protected ShopOrder() { // dla Hibernate
    }

    public ShopOrder(String orderNumber, Customer customer, String note) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.note = note;
        this.status = OrderStatus.NEW;
    }

    /** Dodaje pozycję z ceną podaną przez SERWIS (z produktu), ustawiając obie strony relacji (Rel02, sekcja 1). */
    public OrderLine addLine(Product product, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Ilość musi być dodatnia: " + quantity);
        }
        OrderLine line = new OrderLine(this, product, quantity, unitPrice);
        lines.add(line);
        return line;
    }

    public BigDecimal total() {
        return lines.stream().map(OrderLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    /** Uwagę klient może zmienić — to jedyne pole zamówienia zmieniane z żądania (sekcja 4). */
    public void setNote(String note) {
        this.note = note;
    }

    /** Widok tylko do odczytu — zmiany wyłącznie przez addLine (encja pilnuje reguł). */
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
        return "ShopOrder[id=" + id + ", orderNumber=" + orderNumber + "]";
    }
}
