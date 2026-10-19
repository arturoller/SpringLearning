package s18_aop.aop03_custom_annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Aspekt pomiaru czasu dla {@code @MeasureTime} — na metodzie albo na klasie. {@code @Order(2)}: działa WEWNĄTRZ
 * AuditAspect ({@code @Order(1)}), więc mierzy samą metodę, bez czasu audytu.
 * Czas bierze z wstrzykniętego zegara (Clock), a nie z System.currentTimeMillis() — test podmienia zegar i dostaje
 * przewidywalny wynik.
 */
@Aspect
@Component
@Order(2)
public class MeasureTimeAspect {

    private final Clock clock;
    private final AuditJournal journal;

    public MeasureTimeAspect(Clock clock, AuditJournal journal) {
        this.clock = clock;
        this.journal = journal;
    }

    /** Adnotacja na METODZIE: {@code @annotation} widzi tylko adnotacje metod. */
    @Around("@annotation(measureTime)")
    public Object measureMethod(ProceedingJoinPoint joinPoint, MeasureTime measureTime) throws Throwable {
        return measure(joinPoint, measureTime);
    }

    /**
     * Adnotacja na KLASIE: {@code @within(measureTime)} (within = wewnątrz) — metody klasy oznaczonej adnotacją.
     * {@code !@annotation(...)} wyklucza metody z własną {@code @MeasureTime}, żeby nie mierzyć ich dwa razy
     * (ustawienie metody wygrywa z ustawieniem klasy).
     */
    @Around("@within(measureTime) && !@annotation(s18_aop.aop03_custom_annotations.MeasureTime)")
    public Object measureClass(ProceedingJoinPoint joinPoint, MeasureTime measureTime) throws Throwable {
        return measure(joinPoint, measureTime);
    }

    private Object measure(ProceedingJoinPoint joinPoint, MeasureTime measureTime) throws Throwable {
        long start = clock.millis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsed = clock.millis() - start;
            String verdict = elapsed > measureTime.warnAboveMillis() ? "WOLNO" : "OK";
            journal.add("POMIAR " + joinPoint.getSignature().getName() + ": " + elapsed + " ms (" + verdict
                    + ", próg " + measureTime.warnAboveMillis() + " ms)");
        }
    }
}
