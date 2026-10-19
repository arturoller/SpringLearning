package s18_aop.solutions.aop01_aspects;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import s18_aop.aop01_aspects.ProductNotFoundException;

/**
 * Rozwiązanie wzorcowe ćwiczenia 4 lekcji Aop01Aspects. {@code @AfterThrowing} nie może „połknąć” wyjątku,
 * ale może rzucić INNY — wtedy to on leci do wołającego (tak działa tłumaczenie wyjątków).
 * Rzucamy wyjątek niekontrolowany: kontrolowanego (checked), którego metoda nie deklaruje w throws, proxy
 * nie może przepuścić wprost i opakowałoby go w UndeclaredThrowableException.
 */
@Aspect
public class Aop01Exercise4AspectSolution {

    @AfterThrowing(pointcut = "execution(* s18_aop.aop01_aspects.ProductCatalog.*(..))", throwing = "error")
    public void translate(IllegalArgumentException error) {
        throw new ProductNotFoundException("Brak produktu w katalogu (" + error.getMessage() + ")", error);
    }
}
