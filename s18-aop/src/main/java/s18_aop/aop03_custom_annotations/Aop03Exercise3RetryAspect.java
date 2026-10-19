package s18_aop.aop03_custom_annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/** ĆWICZENIE 3 (trudniejsze) lekcji Aop03CustomAnnotations — opis w klasie lekcji. Bez {@code @Component}. */
@Aspect
public class Aop03Exercise3RetryAspect {

    @Around("@annotation(retryOnFailure)")
    public Object retry(ProceedingJoinPoint joinPoint, RetryOnFailure retryOnFailure) throws Throwable {
        // TODO: wywołuj joinPoint.proceed() najwyżej retryOnFailure.attempts() razy.
        //       Ponawiaj TYLKO po TemporaryFailureException; każdy inny wyjątek rzuć od razu.
        //       Gdy próby się skończą — rzuć ostatni TemporaryFailureException.
        throw new UnsupportedOperationException("TODO");
    }
}
