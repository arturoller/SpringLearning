package s00_start.start08_debugging;

import java.util.List;

/**
 * Małe typy sklepu używane w lekcji Start08Debugging (do celowo zepsutych konfiguracji) i w ćwiczeniu 2.
 * To zwykłe klasy bez adnotacji — żadna z nich nie jest beanem, dopóki konfiguracja jej nie zarejestruje.
 */
public final class DebugShop {

    private DebugShop() {
    }

    /** Kanał powiadomień o zamówieniu (notifier = powiadamiacz). */
    public interface Notifier {
        String channel();
    }

    public static class EmailNotifier implements Notifier {
        @Override
        public String channel() {
            return "EMAIL";
        }
    }

    public static class SmsNotifier implements Notifier {
        @Override
        public String channel() {
            return "SMS";
        }
    }

    /** Serwis zamówień — potrzebuje JEDNEGO powiadamiacza (zależność z konstruktora). */
    public static class OrderService {
        private final Notifier notifier;

        public OrderService(Notifier notifier) {
            this.notifier = notifier;
        }

        public String channel() {
            return notifier.channel();
        }
    }

    /** Źródło cen — interfejs, którego w lekcji NIKT nie implementuje, więc bean tego typu nie istnieje. */
    public interface PriceSource {
        long priceGrosze(String sku);
    }

    /** Raport cen — wymaga PriceSource. */
    public record PriceReport(PriceSource source) {
    }

    /** Dane sklepu z konfiguracji (waluta pochodzi z właściwości shop.currency). */
    public record ShopInfo(String currency) {
    }

    /** Stan magazynu — jego utworzenie celowo kończy się wyjątkiem. */
    public record StockSnapshot(int units) {
    }

    /** Zapis ramek stosu (stack frames) z miejsca, w którym kontener wywołał metodę @Bean. */
    public record StackSnapshot(List<String> frames) {
    }
}
