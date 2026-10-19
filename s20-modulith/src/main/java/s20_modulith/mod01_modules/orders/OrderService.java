package s20_modulith.mod01_modules.orders;

import org.springframework.stereotype.Service;
import s20_modulith.mod01_modules.customers.CustomerDirectory;
import s20_modulith.mod01_modules.customers.CustomerView;
import s20_modulith.mod01_modules.orders.internal.OrderNumbers;
import s20_modulith.mod01_modules.products.ProductCatalog;
import s20_modulith.mod01_modules.products.ProductView;

import java.math.BigDecimal;

/**
 * API modułu {@code orders}: składanie zamówień. Korzysta z API dwóch innych modułów ({@code ProductCatalog},
 * {@code CustomerDirectory}) — wyłącznie z typów w ich pakietach bazowych, nigdy z ich {@code internal}.
 * Własny szczegół ({@code OrderNumbers} z {@code orders.internal}) może używać bez ograniczeń.
 */
@Service
public class OrderService {

    private final ProductCatalog catalog;
    private final CustomerDirectory customers;
    private final OrderNumbers numbers;

    public OrderService(ProductCatalog catalog, CustomerDirectory customers, OrderNumbers numbers) {
        this.catalog = catalog;
        this.customers = customers;
        this.numbers = numbers;
    }

    /**
     * Składa zamówienie: sprawdza klienta i dostępność towaru, nadaje numer i liczy wartość.
     *
     * @throws IllegalArgumentException gdy nie ma klienta albo produktu
     * @throws IllegalStateException    gdy na stanie jest za mało sztuk
     */
    public OrderConfirmation placeOrder(long customerId, String sku, int quantity) {
        CustomerView customer = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma klienta o id " + customerId));
        ProductView product = catalog.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma produktu " + sku));
        if (!catalog.isAvailable(sku, quantity)) {
            throw new IllegalStateException("Za mało sztuk na stanie: " + sku);
        }
        BigDecimal total = product.price().multiply(BigDecimal.valueOf(quantity));
        return new OrderConfirmation(numbers.next(), customer.name(), sku, quantity, total);
    }
}
