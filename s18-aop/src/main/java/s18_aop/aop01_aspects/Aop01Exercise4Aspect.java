package s18_aop.aop01_aspects;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;

/** ĆWICZENIE 4 (trudniejsze) lekcji Aop01Aspects — opis w klasie lekcji. Bez {@code @Component} (jak w ćwiczeniu 1). */
@Aspect
public class Aop01Exercise4Aspect {

    @AfterThrowing(pointcut = "execution(* s18_aop.aop01_aspects.ProductCatalog.*(..))", throwing = "error")
    public void translate(IllegalArgumentException error) {
        // TODO: rzuć ProductNotFoundException z komunikatem "Brak produktu w katalogu (" + error.getMessage() + ")"
        //       i z przyczyną (cause) = error
        throw new UnsupportedOperationException("TODO");
    }
}
