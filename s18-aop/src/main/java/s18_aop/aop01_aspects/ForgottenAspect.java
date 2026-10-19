package s18_aop.aop01_aspects;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * CELOWO NIEDZIAŁAJĄCY aspekt (pułapka z sekcji 2): ma {@code @Aspect}, ale NIE ma {@code @Component}.
 * {@code @Aspect} nie jest stereotypem Springa, więc skanowanie komponentów go pomija: nie powstaje bean,
 * a Spring szuka porad tylko w beanach. Skutek: porada nigdy się nie wykonuje — bez błędu i bez ostrzeżenia.
 */
@Aspect
public class ForgottenAspect {

    private final CallJournal journal;

    public ForgottenAspect(CallJournal journal) {
        this.journal = journal;
    }

    @Before("execution(* s18_aop.aop01_aspects.ProductCatalog.findStock(..))")
    public void neverRuns() {
        journal.add("ZAPOMNIANY aspekt zadziałał");
    }
}
