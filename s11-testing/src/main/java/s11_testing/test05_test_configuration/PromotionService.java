package s11_testing.test05_test_configuration;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

/**
 * PromotionService = promocje sklepu zależne od DATY. „Czarny weekend” trwa od piątku 27.11.2026 do poniedziałku
 * 30.11.2026 (włącznie) i daje 20% rabatu. Datę bierze z WSTRZYKNIĘTEGO zegara ({@link Clock}), nigdy z
 * {@code LocalDate.now()} — dzięki temu test może „przestawić czas” (sekcja 2 lekcji).
 */
@Service
public class PromotionService {

    static final LocalDate BLACK_WEEKEND_START = LocalDate.of(2026, 11, 27);
    static final LocalDate BLACK_WEEKEND_END = LocalDate.of(2026, 11, 30);
    private static final BigDecimal DISCOUNTED = new BigDecimal("0.80");

    private final Clock clock;

    public PromotionService(Clock clock) {
        this.clock = clock;
    }

    /** Czy DZIŚ (według zegara) trwa czarny weekend? */
    public boolean isBlackWeekend() {
        LocalDate today = LocalDate.now(clock);
        return !today.isBefore(BLACK_WEEKEND_START) && !today.isAfter(BLACK_WEEKEND_END);
    }

    /** Cena po promocji: w czarny weekend −20% (zaokrąglone do groszy), poza nim cena bez zmian. */
    public BigDecimal priceFor(BigDecimal basePrice) {
        return isBlackWeekend() ? basePrice.multiply(DISCOUNTED).setScale(2, RoundingMode.HALF_UP) : basePrice;
    }
}
