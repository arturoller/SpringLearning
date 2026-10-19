package s18_aop.aop01_aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.List;

/**
 * ĆWICZENIE 1 (łatwe) lekcji Aop01Aspects — opis w klasie lekcji. Aspekt NIE ma {@code @Component}: test zakłada go
 * ręcznie na katalog (AspectJProxyFactory), więc nierozwiązane ćwiczenie nie wpływa na testy lekcji.
 */
@Aspect
public class Aop01Exercise1Aspect {

    private final List<String> calls;

    public Aop01Exercise1Aspect(List<String> calls) {
        this.calls = calls;
    }

    @Before("execution(* s18_aop.aop01_aspects.ProductCatalog.find*(..))")
    public void recordCall(JoinPoint joinPoint) {
        // TODO: dopisz do listy calls wpis w formacie "wywołanie: findPrice[SPO-001]"
        //       (nazwa metody z joinPoint.getSignature().getName(), argumenty z Arrays.toString(joinPoint.getArgs()))
        throw new UnsupportedOperationException("TODO");
    }
}
