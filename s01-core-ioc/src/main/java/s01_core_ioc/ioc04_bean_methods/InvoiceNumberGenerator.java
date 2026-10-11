package s01_core_ioc.ioc04_bean_methods;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generator numerów faktur, np. "FV/2026/10/0001". Klasa NIE ma adnotacji — beana z niej robi metoda
 * {@code @Bean} w {@link ShopConfig}. Czas pobiera z wstrzykniętego zegara ({@code Clock}), a nie z
 * {@code LocalDate.now()}, więc w teście można go zatrzymać na dowolnej dacie.
 */
public class InvoiceNumberGenerator {

    private static final DateTimeFormatter PREFIX_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Clock clock;
    // counter = licznik; AtomicInteger = bezpieczny przy wielu wątkach (bean jest singletonem, wspólnym dla wszystkich)
    private final AtomicInteger counter = new AtomicInteger();

    public InvoiceNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    /** Następny numer: prefiks z bieżącej daty zegara + czterocyfrowy licznik. W prawdziwym sklepie licznik trzymałaby baza. */
    public String next() {
        return "FV/" + LocalDate.now(clock).format(PREFIX_FORMAT) + "/" + String.format("%04d", counter.incrementAndGet());
    }
}
