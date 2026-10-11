package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ShopOrder = zamówienie (tabela orders). Celowo bez listy pozycji — w tej lekcji liczy się OrderLine jako encja
 * łącząca zamówienie z produktem (relację dwukierunkową z listą pokazuje Rel02OneToMany).
 */
@Entity
@Table(name = "orders")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    protected ShopOrder() {
    }

    public ShopOrder(String customerName) {
        this.customerName = customerName;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }
}
