package s18_aop.aop01_aspects;

import jakarta.annotation.security.RolesAllowed;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Pokaz wyrażeń punktów przecięcia: każda porada zapisuje wpis {@code "PC <wyrażenie>: <metoda>"}, a test sekcji 5
 * sprawdza, które metody złapało które wyrażenie. Wszystkie porady są typu {@code @Before}, a kolejność porad
 * TEGO SAMEGO typu w jednym aspekcie jest nieokreślona — dlatego test nie sprawdza ich kolejności.
 */
@Aspect
@Component
public class PointcutDemoAspect {

    private final CallJournal journal;

    public PointcutDemoAspect(CallJournal journal) {
        this.journal = journal;
    }

    /** execution(...) — wzorzec SYGNATURY metody: typ zwracany, klasa, nazwa, parametry. */
    @Before("s18_aop.aop01_aspects.ShopPointcuts.catalogFinders()")
    public void byExecution(JoinPoint joinPoint) {
        journal.add("PC execution(find*): " + joinPoint.getSignature().getName());
    }

    /**
     * args(sku) — łączenie wyrażeń ({@code &&}) i WIĄZANIE argumentu: parametr {@code String sku} porady dostaje
     * wartość argumentu metody. Przy okazji zawęża: pasują tylko metody z JEDNYM argumentem typu String.
     */
    @Before("s18_aop.aop01_aspects.ShopPointcuts.catalogFinders() && args(sku)")
    public void byArgs(JoinPoint joinPoint, String sku) {
        journal.add("PC args(sku): " + joinPoint.getSignature().getName() + " sku=" + sku);
    }

    /** within(...) — wszystkie metody zadeklarowane w podanym typie (albo w pakiecie: {@code within(pakiet..*)}). */
    @Before("s18_aop.aop01_aspects.ShopPointcuts.inOrderService()")
    public void byWithin(JoinPoint joinPoint) {
        journal.add("PC within(OrderService): " + joinPoint.getSignature().getName());
    }

    /** bean(...) — po NAZWIE beana (tylko w Spring AOP; czysty AspectJ tego nie zna). Działa też {@code bean(*Service)}. */
    @Before("bean(orderService)")
    public void byBeanName(JoinPoint joinPoint) {
        journal.add("PC bean(orderService): " + joinPoint.getSignature().getName());
    }

    /** {@code @annotation(...)} — metody oznaczone adnotacją (pełna nazwa z pakietem!). */
    @Before("s18_aop.aop01_aspects.ShopPointcuts.managerOnly()")
    public void byAnnotation(JoinPoint joinPoint) {
        journal.add("PC @annotation(RolesAllowed): " + joinPoint.getSignature().getName());
    }

    /**
     * CELOWO ZŁA porada (pułapka z sekcji 5): wyrażenie to TEKST, a import na górze pliku
     * ({@link RolesAllowed}) go nie obejmuje. Krótka nazwa „RolesAllowed” nie wskazuje adnotacji z pakietu
     * jakarta.annotation.security, więc ta porada NIGDY się nie uruchamia — bez błędu i bez ostrzeżenia.
     * (Krótka nazwa działa tylko dla typów z pakietu aspektu i z java.lang.) Sprawdzone testem sekcji 5.
     */
    @Before("within(s18_aop.aop01_aspects..*) && @annotation(RolesAllowed)")
    public void importTrap(JoinPoint joinPoint) {
        journal.add("PC PUŁAPKA import: " + joinPoint.getSignature().getName());
    }
}
