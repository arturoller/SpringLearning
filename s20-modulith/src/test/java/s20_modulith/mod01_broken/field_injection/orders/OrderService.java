package s20_modulith.mod01_broken.field_injection.orders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import s20_modulith.mod01_broken.field_injection.products.ProductCatalog;

/**
 * Wstrzykiwanie przez pole ({@code @Autowired} = wstrzyknij automatycznie) — zależność jest dozwolona (API innego
 * modułu), ale sposób wstrzyknięcia nie.
 */
@Service
public class OrderService {

    @Autowired
    private ProductCatalog catalog;

    public boolean canOrder(String sku) {
        return catalog.exists(sku);
    }
}
