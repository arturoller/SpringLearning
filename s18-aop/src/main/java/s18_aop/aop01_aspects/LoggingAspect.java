package s18_aop.aop01_aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspekt z PIĘCIOMA rodzajami porad przypiętymi do tej samej metody: ProductCatalog.findPrice(..).
 * Każda porada zapisuje wpis z przedrostkiem „LOG” — test sekcji 3 sprawdza ich kolejność.
 * {@code @Aspect} (aspekt) mówi: „ta klasa zawiera porady”; {@code @Component} (komponent) mówi: „zrób z niej bean”.
 * Potrzebne są OBA (sekcja 2).
 */
@Aspect
@Component
public class LoggingAspect {

    private final CallJournal journal;

    public LoggingAspect(CallJournal journal) {
        this.journal = journal;
    }

    /** Nazwany punkt przecięcia (pointcut) — porady niżej używają go przez nazwę {@code findPrice()}. Ciało jest puste. */
    @Pointcut("execution(* s18_aop.aop01_aspects.ProductCatalog.findPrice(..))")
    void findPrice() {
    }

    /** before = przed: biegnie PRZED metodą. JoinPoint (punkt złączenia) mówi, która metoda i z jakimi argumentami. */
    @Before("findPrice()")
    public void before(JoinPoint joinPoint) {
        journal.add("LOG @Before: " + joinPoint.getSignature().getName() + " " + Arrays.toString(joinPoint.getArgs()));
    }

    /** after returning = po zwróceniu wyniku: tylko gdy metoda skończyła się NORMALNIE; widzi wynik. */
    @AfterReturning(pointcut = "findPrice()", returning = "result")
    public void afterReturning(Object result) {
        journal.add("LOG @AfterReturning: wynik " + result);
    }

    /** after throwing = po rzuceniu wyjątku: tylko gdy metoda rzuciła wyjątek (typu parametru albo jego podtypu). */
    @AfterThrowing(pointcut = "findPrice()", throwing = "error")
    public void afterThrowing(RuntimeException error) {
        journal.add("LOG @AfterThrowing: " + error.getMessage());
    }

    /** after = po: ZAWSZE po metodzie — po sukcesie i po wyjątku (jak blok finally). */
    @After("findPrice()")
    public void after() {
        journal.add("LOG @After: findPrice zakończona");
    }

    /**
     * around = wokół: porada „owija” metodę i sama decyduje, czy i kiedy ją wywołać: joinPoint.proceed()
     * (proceed = kontynuuj). Musi ZWRÓCIĆ wynik — inaczej wołający dostanie null (sekcja 4).
     */
    @Around("findPrice()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        journal.add("LOG @Around: przed");
        try {
            Object result = joinPoint.proceed();
            journal.add("LOG @Around: po, wynik " + result);
            return result;
        } catch (Throwable error) {
            journal.add("LOG @Around: wyjątek " + error.getClass().getSimpleName());
            throw error; // zapisujemy i RZUCAMY DALEJ — nie połykamy wyjątku (s18_aop/Aop03CustomAnnotations)
        }
    }
}
