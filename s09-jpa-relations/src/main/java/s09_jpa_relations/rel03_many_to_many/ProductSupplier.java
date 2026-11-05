package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * ENCJA POŚREDNIA (sekcja 4 lekcji): „produkt X kupujemy u dostawcy Y po cenie Z”. Zamiast {@code @ManyToMany}
 * (które nie ma gdzie trzymać ceny) — dwa zwykłe {@code @ManyToOne} i własne pole z danymi powiązania.
 * Własne id + UNIQUE na parze (product_id, supplier_id) pilnuje, że para występuje raz.
 */
@Entity
@Table(name = "product_supplier",
        // unique constraint = ograniczenie unikalności; tu tylko opis dla Hibernate — prawdziwe UNIQUE jest w migracji V1
        uniqueConstraints = @UniqueConstraint(name = "uk_product_supplier", columnNames = {"product_id", "supplier_id"}))
public class ProductSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    protected ProductSupplier() { // dla Hibernate
    }

    public ProductSupplier(Product product, Supplier supplier, BigDecimal purchasePrice) {
        this.product = product;
        this.supplier = supplier;
        this.purchasePrice = purchasePrice;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductSupplier other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return ProductSupplier.class.hashCode();
    }

    @Override
    public String toString() {
        return "ProductSupplier[id=" + id + ", purchasePrice=" + purchasePrice + "]";
    }
}
