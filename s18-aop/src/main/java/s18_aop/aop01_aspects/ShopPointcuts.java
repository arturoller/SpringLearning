package s18_aop.aop01_aspects;

import org.aspectj.lang.annotation.Pointcut;

/**
 * Wspólne, nazwane punkty przecięcia (pointcut = punkt przecięcia) — „słownik” wyrażeń wielokrotnego użytku.
 * To ZWYKŁA klasa: nie jest beanem i nie musi mieć {@code @Aspect} — wystarczą metody z {@code @Pointcut}.
 * Aspekty odwołują się do nich pełną nazwą, np. {@code "s18_aop.aop01_aspects.ShopPointcuts.catalogFinders()"}.
 * Zaleta: wyrażenie poprawiasz w JEDNYM miejscu, a nazwa metody mówi, co ono znaczy.
 */
public class ShopPointcuts {

    /** Każda metoda katalogu, której nazwa zaczyna się od „find”, z dowolnymi parametrami. */
    @Pointcut("execution(* s18_aop.aop01_aspects.ProductCatalog.find*(..))")
    public void catalogFinders() {
    }

    /** Każda metoda zadeklarowana w klasie OrderService. */
    @Pointcut("within(s18_aop.aop01_aspects.OrderService)")
    public void inOrderService() {
    }

    /**
     * Metody oznaczone {@code @RolesAllowed} — ALE tylko w pakiecie tej lekcji. Zawężenie within(...) chroni przed
     * „złapaniem” beanów samego Springa (pułapka z sekcji 5, BroadDeprecatedAspect).
     */
    @Pointcut("within(s18_aop.aop01_aspects..*) && @annotation(jakarta.annotation.security.RolesAllowed)")
    public void managerOnly() {
    }
}
