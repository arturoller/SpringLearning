package s10_transactions.tx05_programmatic_transactions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Product = produkt w cenniku sklepu: sku, nazwa i cena w złotych (BigDecimal — bez błędów zaokrągleń). */
@Entity
@Table(name = "products")
public class Product {

    @Id
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Konstruktor bez argumentów — wymagany przez JPA (Hibernate tworzy nim obiekt przy odczycie). */
    protected Product() {
    }

    /**
     * Zmień cenę. Celowo BEZ sprawdzania, czy cena jest dodatnia — pilnuje tego baza (CHECK price > 0), więc zła cena
     * wychodzi dopiero przy zapisie do bazy, czyli przy commicie. Tak lekcja pokazuje błędy zgłaszane przy commicie.
     */
    public void changePrice(BigDecimal newPrice) {
        this.price = newPrice;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
