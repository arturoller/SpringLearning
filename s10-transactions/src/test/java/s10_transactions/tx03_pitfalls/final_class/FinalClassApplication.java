package s10_transactions.tx03_pitfalls.final_class;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CELOWO zepsuta aplikacja do sekcji 3 lekcji s10_transactions/Tx03ProxyPitfalls: skanuje FinalPriceService — klasę
 * {@code final} z {@code @Transactional}. Leży w osobnym pakiecie testów (tx03_pitfalls), bo gdyby była pod
 * tx03_proxy_pitfalls, aplikacja lekcji zeskanowałaby ją i żaden test lekcji by nie wystartował.
 */
@SpringBootApplication
public class FinalClassApplication {
}
