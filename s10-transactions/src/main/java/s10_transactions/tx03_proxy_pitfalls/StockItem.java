package s10_transactions.tx03_proxy_pitfalls;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** StockItem = pozycja magazynowa: produkt (sku, nazwa) i liczba sztuk na stanie (quantity). Tabela stock_items. */
@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int quantity;

    /** Konstruktor bez argumentów — wymagany przez JPA (Hibernate tworzy nim obiekt przy odczycie). */
    protected StockItem() {
    }

    /** Przyjmij dostawę (restock = uzupełnienie zapasu). */
    public void add(int pieces) {
        quantity += pieces;
    }

    /** Zdejmij ze stanu (rezerwacja pod zamówienie). Za mało sztuk → IllegalStateException. */
    public void take(int pieces) {
        if (pieces > quantity) {
            throw new IllegalStateException("Za mało " + sku + ": jest " + quantity + ", potrzeba " + pieces);
        }
        quantity -= pieces;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
