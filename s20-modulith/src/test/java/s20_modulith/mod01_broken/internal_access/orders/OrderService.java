package s20_modulith.mod01_broken.internal_access.orders;

import org.springframework.stereotype.Service;
import s20_modulith.mod01_broken.internal_access.products.internal.ProductStore;

/**
 * BŁĄD (celowy): moduł orders wstrzykuje {@code ProductStore} z {@code products.internal} — szczegół innego modułu.
 * Kompilator na to pozwala (klasa jest public), Spring też (to zwykły bean). Zatrzyma to dopiero
 * {@code ApplicationModules.verify()}.
 */
@Service
public class OrderService {

    private final ProductStore store;

    public OrderService(ProductStore store) {
        this.store = store;
    }

    public boolean canOrder(String sku, int quantity) {
        return store.stockOf(sku) >= quantity;
    }
}
