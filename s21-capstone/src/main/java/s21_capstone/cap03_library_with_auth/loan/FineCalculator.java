package s21_capstone.cap03_library_with_auth.loan;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Kara za spóźnienie: liczba dni po terminie × stawka dzienna. Czysta funkcja — dzień „dziś” podaje wołający
 * (z zegara), więc test jednostkowy sprawdza ją bez Springa i bez czekania.
 */
@Component
public class FineCalculator {

    private final LibraryRules rules;

    public FineCalculator(LibraryRules rules) {
        this.rules = rules;
    }

    /** Ile dni po terminie {@code dueOn} jest dzień {@code day} (0, gdy w terminie albo wcześniej). */
    public long daysOverdue(LocalDate dueOn, LocalDate day) {
        return Math.max(0, ChronoUnit.DAYS.between(dueOn, day));
    }

    /** Kara na dzień {@code day}: 4 dni × 0.50 = 2.00. W dniu terminu jeszcze 0. */
    public BigDecimal fineFor(LocalDate dueOn, LocalDate day) {
        return rules.finePerDay().multiply(BigDecimal.valueOf(daysOverdue(dueOn, day))).setScale(2);
    }
}
