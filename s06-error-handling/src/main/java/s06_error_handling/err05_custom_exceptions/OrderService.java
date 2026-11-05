package s06_error_handling.err05_custom_exceptions;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Warstwa serwisu (service = usługa, logika biznesowa): zna reguły sklepu i rzuca wyjątki domenowe, ale NIC nie wie o HTTP —
 * w całym pliku nie ma {@code HttpStatus}, {@code ResponseStatusException} ani żadnej adnotacji webowej. Dzięki temu da się go
 * użyć w zadaniu wsadowym i przetestować zwykłym testem jednostkowym (bez Springa).
 * Dane trzymamy w pamięci: produkt 1 (Kawa, 5 sztuk), produkt 2 (Herbata, 0 sztuk), zamówienie 1 (niewysłane), zamówienie 2 (wysłane).
 */
@Service
public class OrderService {

    static final int MINIMUM_QUANTITY = 2;   // przykładowa reguła: zamówienie od 2 sztuk

    private final Map<Long, Product> products = new ConcurrentHashMap<>(Map.of(
            1L, new Product(1, "Kawa", 5),
            2L, new Product(2, "Herbata", 0)));
    private final Map<Long, Order> orders = new ConcurrentHashMap<>(Map.of(
            1L, new Order(1, 1, 2, false),
            2L, new Order(2, 1, 3, true)));
    private final AtomicLong nextOrderId = new AtomicLong(10);

    /** Wersja "pytająca": brak produktu to zwykły przypadek, więc wynik jest {@code Optional}, a nie wyjątkiem. */
    public Optional<Product> findProduct(long id) {
        return Optional.ofNullable(products.get(id));
    }

    /** Wersja "wymagająca": wołający oczekuje, że produkt ISTNIEJE — jego brak jest błędem. */
    public Product getProduct(long id) {
        return findProduct(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Order getOrder(long id) {
        return Optional.ofNullable(orders.get(id)).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public Order placeOrder(long productId, int quantity) {
        Product product = getProduct(productId);
        if (quantity < MINIMUM_QUANTITY) {
            throw new MinimumOrderValueException(MINIMUM_QUANTITY);
        }
        if (quantity > product.stock()) {
            throw new OutOfStockException(product.name(), quantity, product.stock());
        }
        products.put(productId, new Product(productId, product.name(), product.stock() - quantity));
        Order order = new Order(nextOrderId.getAndIncrement(), productId, quantity, false);
        orders.put(order.id(), order);
        return order;
    }

    public Order ship(long orderId) {
        Order order = getOrder(orderId);
        if (order.shipped()) {
            throw new OrderAlreadyShippedException(orderId);
        }
        Order shipped = new Order(order.id(), order.productId(), order.quantity(), true);
        orders.put(orderId, shipped);
        return shipped;
    }
}
