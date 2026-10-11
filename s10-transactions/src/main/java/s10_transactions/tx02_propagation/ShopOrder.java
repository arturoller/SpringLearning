package s10_transactions.tx02_propagation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ShopOrder = zamówienie w sklepie (nie „Order” — order to słowo kluczowe SQL: ORDER BY). Tabela orders.
 * IDENTITY: wiersz trafia do bazy (INSERT) od razu przy zapisie — ale w transakcji jest NIEZATWIERDZONY.
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String product;

    @Column(nullable = false)
    private int quantity;

    protected ShopOrder() {
    }

    public ShopOrder(String product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}
