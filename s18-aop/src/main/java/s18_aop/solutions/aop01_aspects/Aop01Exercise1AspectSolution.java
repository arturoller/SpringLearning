package s18_aop.solutions.aop01_aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.Arrays;
import java.util.List;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Aop01Aspects. */
@Aspect
public class Aop01Exercise1AspectSolution {

    private final List<String> calls;

    public Aop01Exercise1AspectSolution(List<String> calls) {
        this.calls = calls;
    }

    @Before("execution(* s18_aop.aop01_aspects.ProductCatalog.find*(..))")
    public void recordCall(JoinPoint joinPoint) {
        calls.add("wywołanie: " + joinPoint.getSignature().getName() + Arrays.toString(joinPoint.getArgs()));
    }
}
