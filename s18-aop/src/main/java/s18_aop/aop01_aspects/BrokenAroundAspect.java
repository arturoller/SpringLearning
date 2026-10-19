package s18_aop.aop01_aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * CELOWO ZEPSUTY aspekt (pułapka z sekcji 4): {@code @Around} bez {@code joinPoint.proceed()}.
 * Porada „owija” metodę, ale jej nie wywołuje — więc ciało metody NIGDY się nie wykonuje, a wołający dostaje
 * to, co zwróciła porada (tu null). Działa tylko na StockReport, żeby nie psuć reszty lekcji.
 */
@Aspect
@Component
public class BrokenAroundAspect {

    private final CallJournal journal;

    public BrokenAroundAspect(CallJournal journal) {
        this.journal = journal;
    }

    @Around("within(s18_aop.aop01_aspects.StockReport)")
    public Object forgotToProceed(ProceedingJoinPoint joinPoint) {
        journal.add("ZEPSUTY @Around: " + joinPoint.getSignature().getName() + " bez proceed()");
        return null; // BŁĄD: brak joinPoint.proceed() — metoda się nie wykona
    }
}
