package s11_testing.test05_test_configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Promocja „Black Week” (tydzień zniżek): od 23 do 29 listopada 2026 r. (włącznie, data w strefie zegara) ceny są
 * niższe o 20%. Serwis NIE woła {@code LocalDate.now()} — pyta wstrzyknięty {@link Clock}, więc test ustala datę
 * co do dnia i wynik nie zależy od tego, kiedy uruchomisz testy.
 */
@Service
public class PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionService.class);

    static final LocalDate START = LocalDate.of(2026, 11, 23);
    static final LocalDate END = LocalDate.of(2026, 11, 29);
    private static final BigDecimal FACTOR = new BigDecimal("0.80");

    private final Clock clock;

    public PromotionService(Clock clock) { // parametr nazywa się "clock" — to ważne w sekcji 1 lekcji
        this.clock = clock;
    }

    /** Czy dziś (według zegara) trwa promocja. */
    public boolean isBlackWeek() {
        LocalDate today = LocalDate.now(clock);
        return !today.isBefore(START) && !today.isAfter(END);
    }

    /** Cena po promocji (20% taniej w czasie Black Week), zaokrąglona do groszy. Zapisuje w logu, gdy rabat działa. */
    public BigDecimal finalPrice(BigDecimal price) {
        if (isBlackWeek()) {
            log.info("Promocja Black Week aktywna — rabat 20% dla ceny {}", price);
            return price.multiply(FACTOR).setScale(2, RoundingMode.HALF_UP);
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    /** Zegar, którego używa serwis — testy sprawdzają, KTÓRY bean {@code Clock} został wstrzyknięty. */
    public Clock clock() {
        return clock;
    }
}
