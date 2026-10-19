package s18_aop.aop03_custom_annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.List;

/** ĆWICZENIE 2 (średnie) lekcji Aop03CustomAnnotations — opis w klasie lekcji. Bez {@code @Component} (jak w ćw. 1). */
@Aspect
public class Aop03Exercise2ErrorLogAspect {

    private final List<String> log;

    public Aop03Exercise2ErrorLogAspect(List<String> log) {
        this.log = log;
    }

    @Around("@annotation(s18_aop.aop03_custom_annotations.AuditLog)")
    public Object logOutcome(ProceedingJoinPoint joinPoint) throws Throwable {
        // TODO: wywołaj metodę. Po sukcesie dopisz do logu "OK placeOrder" (nazwa metody) i zwróć wynik.
        //       Po wyjątku dopisz "BŁĄD placeOrder: IllegalStateException" (prosta nazwa klasy wyjątku)
        //       i rzuć DALEJ ten sam wyjątek — nie połykaj go i nie opakowuj w inny.
        throw new UnsupportedOperationException("TODO");
    }
}
