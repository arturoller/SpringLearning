package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Product = produkt sklepu ze stanem magazynowym (stock = liczba sztuk na stanie). Encja (entity) BEZ kolumny wersji —
 * na niej widać zjawiska izolacji i utraconą aktualizację (lost update). Tabela products.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false)
    private int stock;

    /** Konstruktor bez argumentów — wymagany przez JPA (Hibernate tworzy nim obiekt przy odczycie). */
    protected Product() {
    }

    public Product(String sku, String name, String category, int stock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.stock = stock;
    }

    /** Sprzedaj sztuki: zdejmij ze stanu. Za mało sztuk → IllegalStateException. */
    public void sell(int pieces) {
        if (pieces > stock) {
            throw new IllegalStateException("Za mało " + sku + ": jest " + stock + ", potrzeba " + pieces);
        }
        stock -= pieces;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getStock() {
        return stock;
    }
}
