package s18_aop.aop01_aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * {@code @Around} PODMIENIA ARGUMENTY: {@code proceed(noweArgumenty)} wywołuje metodę z innymi wartościami.
 * Tu: SKU z zamówienia jest oczyszczane ze spacji i zamieniane na wielkie litery („ spo-001 ” → „SPO-001”).
 * To pokaz możliwości — w prawdziwym kodzie taka zmiana jest „niewidzialna” dla czytającego OrderService,
 * więc używaj jej rzadko (patrz DOBRA PRAKTYKA w sekcji 4 lekcji).
 */
@Aspect
@Component
public class SkuNormalizingAspect {

    private final CallJournal journal;

    public SkuNormalizingAspect(CallJournal journal) {
        this.journal = journal;
    }

    /** args(sku, quantity) wiąże OBA argumenty metody z parametrami porady (nazwy muszą się zgadzać). */
    @Around("execution(* s18_aop.aop01_aspects.OrderService.placeOrder(..)) && args(sku, quantity)")
    public Object normalizeSku(ProceedingJoinPoint joinPoint, String sku, int quantity) throws Throwable {
        String normalized = sku.strip().toUpperCase(Locale.ROOT);
        journal.add("NORM '" + sku + "' -> '" + normalized + "'");
        return joinPoint.proceed(new Object[] {normalized, quantity}); // tablica: wszystkie argumenty, w kolejności
    }
}
