package s10_transactions.tx03_proxy_pitfalls;

import org.springframework.transaction.annotation.Transactional;

/**
 * FinalPriceService = klasa FINAL z metodą {@code @Transactional}. CELOWO bez {@code @Service}: gdyby była beanem,
 * Tx03Application by nie wystartowała — CGLIB nie umie zrobić podklasy klasy final. Test lekcji rejestruje ją
 * w osobnym, małym kontekście i sprawdza błąd startu.
 */
public final class FinalPriceService {

    @Transactional
    public long priceOf(String product) {
        return "Kawa".equals(product) ? 2999 : 0;
    }
}
