package s20_modulith.mod01_modules.products;

import org.springframework.stereotype.Service;
import s20_modulith.mod01_modules.products.internal.ProductStore;

import java.util.Optional;

/**
 * API modułu {@code products}: katalog produktów (catalog = katalog). Publiczna klasa w PAKIECIE BAZOWYM modułu —
 * inne moduły mogą jej używać. Szczegóły (gdzie i jak trzymamy produkty) są w podpakiecie {@code internal}, do
 * którego inne moduły nie mają wstępu — pilnuje tego {@code ApplicationModules.verify()}.
 */
@Service
public class ProductCatalog {

    private final ProductStore store;

    public ProductCatalog(ProductStore store) {
        this.store = store;
    }

    /** Produkt o podanym SKU albo pusty Optional, gdy takiego nie ma. */
    public Optional<ProductView> findBySku(String sku) {
        return store.find(sku);
    }

    /** Czy produkt istnieje i ma na stanie co najmniej {@code quantity} sztuk. */
    public boolean isAvailable(String sku, int quantity) {
        return store.find(sku).map(p -> p.stock() >= quantity).orElse(false);
    }
}
