package s06_error_handling.err05_custom_exceptions;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Serwis zamówień: logika biznesowa rzuca wyjątki DOMENY (ShopException) — nie zna kodów HTTP ani ProblemDetail.
 * Stan magazynu w pamięci (dane sklepu z kursu): SKU → liczba sztuk.
 */
@Service
public class OrderService {

    private static final Map<String, Integer> STOCK = Map.of(
            "ELE-001", 7, "ELE-002", 0, "ELE-003", 25, "KSI-001", 15, "KSI-003", 3);

    /** Stan jednego produktu albo ProductNotFoundException. */
    public int stockOf(String sku) {
        Integer stock = STOCK.get(sku);
        if (stock == null) {
            throw new ProductNotFoundException(sku);
        }
        return stock;
    }

    /** Składanie zamówienia: reguły biznesowe sprawdzane po kolei, pierwsze naruszenie przerywa. */
    public String place(NewOrder order) {
        List<OrderLine> lines = order.lines();
        if (lines.isEmpty()) {
            throw new InvalidOrderException("Zamówienie nie ma żadnej pozycji");
        }
        for (OrderLine line : lines) {
            Integer stock = STOCK.get(line.sku());
            if (stock == null) {
                // 422, a nie 404: błędny jest element TREŚCI zapytania, a adres /api/orders istnieje
                throw new InvalidOrderException("Nieznany produkt w pozycji zamówienia: " + line.sku());
            }
            if (line.quantity() > stock) {
                throw new OutOfStockException(line.sku(), stock, line.quantity());
            }
        }
        return "ZAM-001";
    }

    /** Symulacja awarii bazy: niższa warstwa rzuca wyjątek techniczny, my opakowujemy go, zachowując przyczynę. */
    public int salesReport() {
        try {
            throw new IllegalStateException("Connection refused: jdbc:h2:tcp://db-prod:9092/sklep (login: sa)");
        } catch (IllegalStateException e) {
            throw new StorageUnavailableException("Nie udało się odczytać raportu sprzedaży", e);
        }
    }

    /** Błąd programisty (nieprzewidziany): np. NullPointerException głęboko w kodzie. */
    public void bug() {
        Object nothing = null;
        nothing.toString();
    }
}
