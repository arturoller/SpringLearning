package s20_modulith.mod01_broken.cycle.products;

import org.springframework.stereotype.Service;
import s20_modulith.mod01_broken.cycle.orders.OrderHistory;

import java.util.List;

/**
 * BŁĄD (celowy): katalog produktów chce pokazać bestsellery, więc pyta moduł orders o historię zamówień.
 * Ale orders już zależy od products — powstaje cykl products → orders → products.
 */
@Service
public class ProductCatalog {

    private final OrderHistory history;

    public ProductCatalog(OrderHistory history) {
        this.history = history;
    }

    public List<String> bestsellers() {
        return history.mostOrderedSkus();
    }

    public boolean exists(String sku) {
        return sku.startsWith("ELE-");
    }
}
