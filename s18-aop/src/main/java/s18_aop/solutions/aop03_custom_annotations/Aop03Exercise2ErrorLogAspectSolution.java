package s18_aop.solutions.aop03_custom_annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Aop03CustomAnnotations: zapisz i rzuć DALEJ ten sam wyjątek. */
@Aspect
public class Aop03Exercise2ErrorLogAspectSolution {

    private final List<String> log;

    public Aop03Exercise2ErrorLogAspectSolution(List<String> log) {
        this.log = log;
    }

    @Around("@annotation(s18_aop.aop03_custom_annotations.AuditLog)")
    public Object logOutcome(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        try {
            Object result = joinPoint.proceed();
            log.add("OK " + method);
            return result;
        } catch (Throwable error) {
            log.add("BŁĄD " + method + ": " + error.getClass().getSimpleName());
            throw error; // ten sam obiekt wyjątku — wołający widzi prawdziwą przyczynę
        }
    }
}
