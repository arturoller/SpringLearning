package s10_transactions.tx03_proxy_pitfalls;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** StockItem = pozycja magazynowa: produkt i liczba sztuk na stanie. Tabela stock. */
@Entity
@Table(name = "stock")
public class StockItem {

    @Id
    private String product;

    @Column(nullable = false)
    private int quantity;

    protected StockItem() {
    }

    public StockItem(String product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    /** Przyjmij dostawę (restock = uzupełnienie zapasu). */
    public void add(int pieces) {
        quantity += pieces;
    }

    /** Zdejmij ze stanu (rezerwacja pod zamówienie). Za mało sztuk → IllegalStateException. */
    public void take(int pieces) {
        if (pieces > quantity) {
            throw new IllegalStateException("Za mało " + product + ": jest " + quantity + ", potrzeba " + pieces);
        }
        quantity -= pieces;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}
