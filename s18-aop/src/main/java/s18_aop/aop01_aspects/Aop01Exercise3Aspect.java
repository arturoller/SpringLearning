package s18_aop.aop01_aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Map;
import java.util.TreeMap;

/** ĆWICZENIE 3 (średnie) lekcji Aop01Aspects — opis w klasie lekcji. Bez {@code @Component} (jak w ćwiczeniu 1). */
@Aspect
public class Aop01Exercise3Aspect {

    private final Map<String, Integer> counts = new TreeMap<>();

    @Around("execution(* s18_aop.aop01_aspects.ProductCatalog.*(..))")
    public Object countCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        // TODO: zwiększ licznik dla nazwy metody: counts.merge(nazwa, 1, Integer::sum),
        //       potem wywołaj metodę (joinPoint.proceed()) i ZWRÓĆ jej wynik
        throw new UnsupportedOperationException("TODO");
    }

    /** Liczniki wywołań: nazwa metody → ile razy (kopia, posortowana po nazwie). */
    public Map<String, Integer> counts() {
        return new TreeMap<>(counts);
    }
}
