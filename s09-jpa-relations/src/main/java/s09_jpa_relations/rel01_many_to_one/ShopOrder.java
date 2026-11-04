package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Zamówienie — strona „wiele” relacji: wiele zamówień należy do jednego klienta. To ta encja ma pole
 * {@code customer} i to jej tabela ({@code orders}) ma kolumnę klucza obcego {@code customer_id}.
 * Nazwa {@code ShopOrder}, a nie {@code Order}: ORDER to słowo zastrzeżone w SQL ({@code ORDER BY}).
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    @Column(name = "ordered_on", nullable = false)
    private LocalDate orderedOn;

    // many to one = wiele do jednego; fetch = pobieranie; LAZY = leniwie (dopiero przy pierwszym użyciu);
    // optional = false — zamówienie BEZ klienta nie istnieje (Hibernate może wtedy użyć INNER JOIN zamiast LEFT JOIN);
    // join column = kolumna złączenia (klucz obcy); nullable = false — dla ddl-auto i walidacji (NOT NULL w migracji).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    protected ShopOrder() { // dla Hibernate
    }

    public ShopOrder(String orderNumber, LocalDate orderedOn, Customer customer) {
        this.orderNumber = orderNumber;
        this.orderedOn = orderedOn;
        this.customer = customer;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public LocalDate getOrderedOn() {
        return orderedOn;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    /**
     * Celowo BEZ pola {@code customer}: wypisanie klienta w toString wczytałoby go z bazy (osobny SELECT) przy każdym
     * logowaniu zamówienia — PUŁAPKA z sekcji 2 lekcji (sprawdzona testem).
     */
    @Override
    public String toString() {
        return "ShopOrder[id=" + id + ", orderNumber=" + orderNumber + ", orderedOn=" + orderedOn + "]";
    }
}
