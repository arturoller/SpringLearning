package s00_start.start07_glossary;

import java.util.Map;

/**
 * Małe klasy pomocnicze do sekcji 3 lekcji Start07Glossary (pojęcia, które uczeń myli). Test sprawdza na nich,
 * że różnice są prawdziwe, a nie tylko opisane w komentarzu.
 */
public final class MixedUpConcepts {

    private MixedUpConcepts() {
    }

    /** Zwykła klasa — bean powstaje dopiero wtedy, gdy zarejestrujemy ją w kontenerze. */
    public static class Counter {
    }

    /** Zależność, którą w testach zastępujemy dublerami (stock = stan magazynowy). */
    public interface StockChecker {
        int stock(String sku);
    }

    /** Prawdziwa implementacja: stany magazynowe jak w sklepie z kursu (ELE-001 Laptop Pro 14 — 7 szt., ELE-002 Smartfon X — 0). */
    public static class RealStockChecker implements StockChecker {
        private static final Map<String, Integer> STOCK = Map.of("ELE-001", 7, "ELE-002", 0, "SPO-002", 300);

        @Override
        public int stock(String sku) {
            return STOCK.getOrDefault(sku, 0);
        }
    }

    /** Klasa testowana: potrafi odpowiedzieć, czy produkt jest do kupienia. */
    public static class SalesFacade {
        private final StockChecker checker;

        public SalesFacade(StockChecker checker) {
            this.checker = checker;
        }

        public boolean canSell(String sku) {
            return checker.stock(sku) > 0;
        }
    }
}
