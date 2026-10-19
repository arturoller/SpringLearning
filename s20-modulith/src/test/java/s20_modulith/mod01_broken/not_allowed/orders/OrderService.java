package s20_modulith.mod01_broken.not_allowed.orders;

import org.springframework.stereotype.Service;
import s20_modulith.mod01_broken.not_allowed.customers.CustomerDirectory;
import s20_modulith.mod01_broken.not_allowed.products.ProductCatalog;

/**
 * BŁĄD (celowy): {@code package-info.java} tego modułu pozwala tylko na zależność od products, a klasa używa też
 * {@code CustomerDirectory} z modułu customers (np. żeby dać VIP-om rabat).
 */
@Service
public class OrderService {

    private final ProductCatalog catalog;
    private final CustomerDirectory customers;

    public OrderService(ProductCatalog catalog, CustomerDirectory customers) {
        this.catalog = catalog;
        this.customers = customers;
    }

    public boolean canOrder(long customerId, String sku) {
        return catalog.exists(sku) && customers.isVip(customerId);
    }
}
