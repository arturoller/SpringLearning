package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * VersionedProduct = produkt z WERSJĄ (version = numer wersji wiersza). Pole z {@code @Version} włącza blokowanie
 * optymistyczne (optimistic locking): Hibernate przy każdym UPDATE zwiększa wersję i dopisuje do zapytania warunek
 * {@code where version = <wersja z chwili odczytu>}. Jeśli ktoś w międzyczasie zmienił wiersz, UPDATE nie trafi
 * w żaden wiersz i Hibernate zgłosi błąd zamiast po cichu nadpisać cudzą zmianę. Tabela versioned_products.
 */
@Entity
@Table(name = "versioned_products")
public class VersionedProduct {

    @Id
    private String sku;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int stock;

    @Version
    private long version;

    protected VersionedProduct() {
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

    public int getStock() {
        return stock;
    }

    public long getVersion() {
        return version;
    }
}
