package s18_aop.aop03_custom_annotations;

/**
 * Klient „kapryśnego” magazynu (flaky = zawodny) do ćwiczenia 3. Deterministyczny: pierwsze
 * {@code failuresBeforeSuccess} wywołań fetchStock kończy się TemporaryFailureException, kolejne — sukcesem.
 * fetchStockOfUnknown zawsze rzuca IllegalArgumentException (tego NIE wolno ponawiać — powtórka nic nie zmieni).
 */
public class FlakyWarehouseClient {

    private final int failuresBeforeSuccess;
    private int attempts;

    public FlakyWarehouseClient(int failuresBeforeSuccess) {
        this.failuresBeforeSuccess = failuresBeforeSuccess;
    }

    @RetryOnFailure(attempts = 3)
    public int fetchStock(String sku) {
        attempts++;
        if (attempts <= failuresBeforeSuccess) {
            throw new TemporaryFailureException("Magazyn chwilowo niedostępny (próba " + attempts + ")");
        }
        return 120;
    }

    @RetryOnFailure(attempts = 3)
    public int fetchStockOfUnknown(String sku) {
        attempts++;
        throw new IllegalArgumentException("Nieznany SKU: " + sku);
    }

    /** Ile razy naprawdę wywołano metody (licząc ponowienia). */
    public int attempts() {
        return attempts;
    }
}
