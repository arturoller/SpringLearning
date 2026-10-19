package s18_aop.aop01_aspects;

import org.springframework.stereotype.Service;

/**
 * Składanie zamówień. Korzysta z katalogu przez wstrzyknięty bean — czyli przez PROXY katalogu, więc porady
 * przypięte do metod katalogu działają także przy wywołaniu z tej klasy (inaczej niż przy this.metoda() —
 * s18_aop/Aop02Proxies).
 */
@Service
public class OrderService {

    private final ProductCatalog catalog;
    private final CallJournal journal;

    public OrderService(ProductCatalog catalog, CallJournal journal) {
        this.catalog = catalog;
        this.journal = journal;
    }

    public String placeOrder(String sku, int quantity) {
        journal.add("METODA placeOrder(" + sku + ", " + quantity + ")");
        int stock = catalog.findStock(sku);
        if (stock < quantity) {
            throw new IllegalStateException("Brak towaru: " + sku + " (stan " + stock + ", zamówiono " + quantity + ")");
        }
        return "Zamówienie przyjęte: " + quantity + " x " + sku;
    }
}
