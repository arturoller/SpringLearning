package s18_aop.aop03_custom_annotations;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspekt audytu dla metod z {@code @AuditLog}. {@code @Order(1)} (order = kolejność) — mniejsza liczba = wyższy
 * priorytet = aspekt jest „bardziej na zewnątrz”: zaczyna PIERWSZY i kończy OSTATNI. Dzięki temu audyt widzi
 * ostateczny wynik wywołania (także czas zmierzony przez MeasureTimeAspect z {@code @Order(2)} — sekcja 3).
 */
@Aspect
@Component
@Order(1)
public class AuditAspect {

    private final AuditJournal journal;

    public AuditAspect(AuditJournal journal) {
        this.journal = journal;
    }

    /**
     * {@code @annotation(auditLog)} — MAŁA litera: to nazwa PARAMETRU porady, nie typ. Spring dopasuje metody
     * z adnotacją typu parametru (AuditLog) i poda do porady jej instancję — stąd {@code auditLog.action()}.
     */
    @Around("@annotation(auditLog)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        journal.add("AUDYT start " + auditLog.action() + " " + Arrays.toString(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            journal.add("AUDYT OK " + auditLog.action());
            return result;
        } catch (Throwable error) {
            journal.add("AUDYT BŁĄD " + auditLog.action() + ": " + error.getMessage());
            throw error; // DOBRZE: zapisz i rzuć DALEJ ten sam wyjątek (porównaj z SafeCallAspect)
        }
    }

    /** Pułapka z sekcji 1: BrokenAuditLog nie ma {@code @Retention(RUNTIME)} — ta porada nigdy się nie wykona. */
    @Before("@annotation(brokenAuditLog)")
    public void brokenAudit(JoinPoint joinPoint, BrokenAuditLog brokenAuditLog) {
        journal.add("AUDYT (zepsuta adnotacja) " + brokenAuditLog.value());
    }
}
