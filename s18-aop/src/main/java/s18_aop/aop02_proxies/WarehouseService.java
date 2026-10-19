package s18_aop.aop02_proxies;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Magazyn — pokaz, KTÓRE metody proxy CGLIB umie przechwycić (sekcja 5): public, protected, pakietowa (bez
 * modyfikatora), private i final. Pole {@code stock} jest ustawiane w konstruktorze (NIE jako stała), żeby było
 * widać, że obiekt proxy ma puste pola.
 */
@Service
public class WarehouseService {

    private final Map<String, Integer> stock;

    public WarehouseService() {
        this.stock = Map.of("SPO-001", 120, "KSI-001", 15, "ELE-002", 0);
    }

    public int stockOf(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    /** final = ostateczna: podklasa (proxy CGLIB) nie może jej nadpisać, więc porady jej nie obejmą. */
    public final int finalStockOf(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    protected String protectedInfo() {
        return "protected";
    }

    String packageInfo() {
        return "pakietowa";
    }

    public String publicCallingPrivate() {
        return "public → " + privateInfo();
    }

    private String privateInfo() {
        return "private";
    }
}
