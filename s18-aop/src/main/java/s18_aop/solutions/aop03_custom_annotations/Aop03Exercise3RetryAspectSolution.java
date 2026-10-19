package s18_aop.solutions.aop03_custom_annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import s18_aop.aop03_custom_annotations.RetryOnFailure;
import s18_aop.aop03_custom_annotations.TemporaryFailureException;

/**
 * Rozwiązanie wzorcowe ćwiczenia 3 lekcji Aop03CustomAnnotations. W Spring AOP joinPoint.proceed() można wywołać
 * kilka razy — każde wywołanie to nowe przejście przez resztę łańcucha porad i przez metodę.
 */
@Aspect
public class Aop03Exercise3RetryAspectSolution {

    @Around("@annotation(retryOnFailure)")
    public Object retry(ProceedingJoinPoint joinPoint, RetryOnFailure retryOnFailure) throws Throwable {
        TemporaryFailureException lastFailure = null;
        for (int attempt = 1; attempt <= retryOnFailure.attempts(); attempt++) {
            try {
                return joinPoint.proceed();
            } catch (TemporaryFailureException failure) {
                lastFailure = failure; // chwilowa awaria — spróbuj jeszcze raz
            }
            // inne wyjątki nie są łapane, więc lecą od razu do wołającego
        }
        throw lastFailure;
    }
}
