package s18_aop.aop02_proxies;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Aspekt-sonda: liczy wywołania metod beanów, których nazwa kończy się na „Service” albo „Calculator”
 * (tylko w pakietach kursu — zawężenie within, jak radzi s18_aop/Aop01Aspects). Licznik pokazuje, które wywołania
 * przeszły przez proxy, a które je ominęły.
 */
@Aspect
@Component
public class CountingAspect {

    private final CallCounter counter;

    public CountingAspect(CallCounter counter) {
        this.counter = counter;
    }

    @Before("within(s18_aop..*) && (bean(*Service) || bean(*Calculator))")
    public void count(JoinPoint joinPoint) {
        counter.increment(joinPoint.getSignature().getName());
    }
}
