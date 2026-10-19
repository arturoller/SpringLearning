package s18_aop.solutions.aop02_proxies;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import s18_aop.aop02_proxies.PriceCalculator;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń 1 i 3 lekcji Aop02Proxies — zajrzyj dopiero po własnej próbie! */
public final class Aop02ProxiesSolutions {

    private Aop02ProxiesSolutions() {
    }

    static String solution1(Object bean) {
        if (AopUtils.isJdkDynamicProxy(bean)) {
            return "JDK";
        }
        if (AopUtils.isCglibProxy(bean)) {
            return "CGLIB";
        }
        return "BRAK PROXY";
    }

    static PriceCalculator solution3(PriceCalculator target, List<String> log) {
        ProxyFactory factory = new ProxyFactory(target); // target ma interfejs, więc proxy JDK (proxyTargetClass = false)
        factory.addAdvice((MethodInterceptor) invocation -> {
            String name = invocation.getMethod().getName();
            log.add("przed " + name);
            Object result = invocation.proceed(); // jak joinPoint.proceed() w @Around
            log.add("po " + name);
            return result;
        });
        return (PriceCalculator) factory.getProxy();
    }
}
