package s20_modulith.mod01_broken.internal_access.products;

import org.springframework.stereotype.Service;
import s20_modulith.mod01_broken.internal_access.products.internal.ProductStore;

/** API modułu products (zła aplikacja z lekcji Mod01Modules). */
@Service
public class ProductCatalog {

    private final ProductStore store;

    public ProductCatalog(ProductStore store) {
        this.store = store;
    }

    public int stockOf(String sku) {
        return store.stockOf(sku);
    }
}
