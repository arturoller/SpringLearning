package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.transaction.annotation.Transactional;

/**
 * FinalPriceService = klasa FINAL z metodą {@code @Transactional}. CELOWO bez {@code @Service}: gdyby była beanem,
 * Tx03Application by nie wystartowała — CGLIB nie umie zrobić podklasy klasy final. Test lekcji rejestruje ją
 * w osobnym, małym kontekście (ApplicationContextRunner) i sprawdza błąd startu.
 */
public final class FinalPriceService {

    /** Cena w groszach (6499 = 64,99 zł). */
    @Transactional
    public long priceOf(String sku) {
        return "SPO-001".equals(sku) ? 6499 : 0;
    }
}
