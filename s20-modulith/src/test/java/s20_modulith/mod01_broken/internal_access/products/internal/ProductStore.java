package s20_modulith.mod01_broken.internal_access.products.internal;

import org.springframework.stereotype.Component;

/** Szczegół wewnętrzny modułu products (zła aplikacja z lekcji Mod01Modules). */
@Component
public class ProductStore {

    public int stockOf(String sku) {
        return "ELE-001".equals(sku) ? 7 : 0;
    }
}
