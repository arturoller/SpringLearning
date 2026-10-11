package s06_error_handling.err05_custom_exceptions;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serwis zamówień — warstwa biznesowa. Rzuca wyjątki DOMENOWE (ShopException) i nic nie wie o HTTP ani o ProblemDetail.
 * Nie loguje i nie rzuca ponownie ("log and throw" to antywzorzec): loguje raz ShopExceptionAdvice.
 */
@Service
public class OrderService {

    /** Magazyn w pamięci: id produktu → dostępna ilość. Produkt 1 (Kawa): 5 szt., 2 (Herbata): 0 szt. */
    private static final Map<Long, Integer> STOCK = Map.of(1L, 5, 2L, 0);

    private final AtomicLong nextOrderId = new AtomicLong(100);

    public OrderConfirmation place(OrderRequest request) {
        Integer available = STOCK.get(request.productId());
        if (available == null) {
            throw new ProductNotFoundException(request.productId());
        }
        if (request.quantity() > available) {
            throw new OutOfStockException(request.productId(), request.quantity(), available);
        }
        // SYMULACJE na potrzeby lekcji (i pliku .http): w prawdziwym kodzie byłoby tu wywołanie bramki płatności.
        if ("gateway-down".equals(request.customer())) {
            throw new PaymentGatewayException(new IllegalStateException("Connection refused: pay.internal:8443"));
        }
        if ("boom".equals(request.customer())) {
            throw new IllegalStateException("Niespójny stan tabeli ORDERS_V2");   // błąd programisty, nie domenowy
        }
        return new OrderConfirmation(nextOrderId.getAndIncrement(), request.productId(), request.quantity());
    }
}
