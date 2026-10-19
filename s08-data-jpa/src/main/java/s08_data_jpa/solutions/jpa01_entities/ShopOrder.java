package s08_data_jpa.solutions.jpa01_entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import s08_data_jpa.jpa01_entities.OrderStatus;

import java.math.BigDecimal;
import java.util.Objects;

/** Rozwiązanie ćwiczeń 1 i 2 lekcji Jpa01Entities — zajrzyj dopiero po własnej próbie! */
@Entity
@Table(name = "orders")                    // "order" to słowo zastrzeżone SQL — DDL by się nie wykonał
public class ShopOrder {

    @Id                                    // numer nadaje sklep → bez @GeneratedValue
    @Column(length = 20)
    private String number;

    @Enumerated(EnumType.STRING)           // tekst "OPLACONE", nie numer pozycji
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    protected ShopOrder() {
    }

    public ShopOrder(String number, OrderStatus status, BigDecimal total) {
        this.number = number;
        this.status = status;
        this.total = total;
    }

    public String getNumber() {
        return number;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    /** Rozwiązanie 2: klucz biznesowy = numer zamówienia (znany od początku, niezmienny). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShopOrder other)) {
            return false;
        }
        return Objects.equals(number, other.getNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(number);
    }
}
