package s18_aop.aop02_proxies;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Klasa startowa lekcji Aop02Proxies. Skanuje tylko ten pakiet (aop02_proxies).
 * {@code @EnableAspectJAutoProxy(exposeProxy = true)} (expose = udostępnij) — DODATKOWE ustawienie tej lekcji:
 * proxy odkłada się w wątku na czas wywołania, żeby działało AopContext.currentProxy() (sekcja 6).
 * Boot i tak włącza AOP sam; ta adnotacja niczego nie wyłącza — Spring scala ustawienia, więc proxy dalej są CGLIB
 * (proxyTargetClass = true od Boota) i dodatkowo „udostępnione”.
 * Klasy PlainSpringConfig, CheckoutByClass i FinalPriceService nie mają {@code @Component} — rejestrują je tylko testy.
 */
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class Aop02Application {

    public static void main(String[] args) {
        SpringApplication.run(Aop02Application.class, args);
    }
}
