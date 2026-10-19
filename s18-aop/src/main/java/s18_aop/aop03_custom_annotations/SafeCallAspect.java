package s18_aop.aop03_custom_annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * CELOWO ZŁY aspekt (pułapka z sekcji 4): łapie KAŻDY wyjątek, zapisuje go i zwraca null. Wołający nie dowie się
 * o błędzie — dostanie null i wywróci się dużo dalej, w miejscu bez związku z przyczyną. Poprawny wzór: AuditAspect
 * (zapisz i rzuć dalej) albo {@code @AfterThrowing} (ta porada w ogóle nie może połknąć wyjątku).
 */
@Aspect
@Component
public class SafeCallAspect {

    private final AuditJournal journal;

    public SafeCallAspect(AuditJournal journal) {
        this.journal = journal;
    }

    @Around("@annotation(s18_aop.aop03_custom_annotations.SafeCall)")
    public Object swallow(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (Throwable error) {
            journal.add("POŁKNIĘTY " + error.getClass().getSimpleName() + ": " + error.getMessage());
            return null; // BŁĄD: wyjątek znika, a wołający dostaje null
        }
    }
}
