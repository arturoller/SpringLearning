package s18_aop.solutions.aop01_aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Map;
import java.util.TreeMap;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Aop01Aspects. */
@Aspect
public class Aop01Exercise3AspectSolution {

    private final Map<String, Integer> counts = new TreeMap<>();

    @Around("execution(* s18_aop.aop01_aspects.ProductCatalog.*(..))")
    public Object countCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        counts.merge(joinPoint.getSignature().getName(), 1, Integer::sum);
        return joinPoint.proceed(); // bez „return” wołający dostałby null
    }

    public Map<String, Integer> counts() {
        return new TreeMap<>(counts);
    }
}
