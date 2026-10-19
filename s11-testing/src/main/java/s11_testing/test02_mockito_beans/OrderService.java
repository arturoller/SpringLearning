package s11_testing.test02_mockito_beans;

import org.springframework.stereotype.Service;

/**
 * OrderService = serwis zamówień — KLASA TESTOWANA w tej lekcji. Ma dwie zależności: katalog (skąd cena) i
 * powiadamiacz (efekt uboczny). Logika: sprawdź ilość → weź produkt → policz kwotę → powiadom → zwróć potwierdzenie.
 */
@Service
public class OrderService {

    private final ProductCatalog catalog;
    private final OrderNotifier notifier;

    public OrderService(ProductCatalog catalog, OrderNotifier notifier) {
        this.catalog = catalog;
        this.notifier = notifier;
    }

    public OrderConfirmation placeOrder(String customerEmail, String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Ilość musi być dodatnia, a jest: " + quantity);
        }
        Product product = catalog.getBySku(sku);
        var confirmation = new OrderConfirmation(customerEmail, sku, quantity, product.priceGrosze() * quantity);
        notifier.orderPlaced(confirmation);
        return confirmation;
    }
}
