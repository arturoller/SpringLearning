package s20_modulith.mod01_broken.cycle.orders;

import org.springframework.stereotype.Service;
import s20_modulith.mod01_broken.cycle.products.ProductCatalog;

/** Moduł orders zależy od API modułu products — to samo w sobie jest w porządku (zła aplikacja z lekcji Mod01Modules). */
@Service
public class OrderService {

    private final ProductCatalog catalog;

    public OrderService(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    public boolean canOrder(String sku) {
        return catalog.exists(sku);
    }
}
