package s20_modulith.mod01_broken.field_injection.products;

import org.springframework.stereotype.Service;

/** API modułu products (aplikacja z lekcji Mod01Modules). */
@Service
public class ProductCatalog {

    public boolean exists(String sku) {
        return sku.startsWith("ELE-");
    }
}
