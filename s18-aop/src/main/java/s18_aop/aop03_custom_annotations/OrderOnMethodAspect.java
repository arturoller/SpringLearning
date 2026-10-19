package s18_aop.aop03_custom_annotations;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;

/**
 * Pokaz pułapki z sekcji 3: {@code @Order} postawione na METODZIE porady jest ignorowane — liczy się tylko
 * {@code @Order} na KLASIE aspektu (albo interfejs Ordered). Ten aspekt nie ma {@code @Order} na klasie, więc ma
 * najniższy priorytet, mimo że jego metoda „prosi” o -100. Bez {@code @Component}: rejestruje go tylko test.
 */
@Aspect
public class OrderOnMethodAspect {

    private final AuditJournal journal;

    public OrderOnMethodAspect(AuditJournal journal) {
        this.journal = journal;
    }

    @Order(-100) // NIE DZIAŁA na metodzie porady
    @Before("execution(* s18_aop.aop03_custom_annotations.PriceService.changePrice(..))")
    public void wantsToBeFirst() {
        journal.add("PORADA z @Order(-100) na metodzie");
    }
}
