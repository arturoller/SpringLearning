package s18_aop.aop01_aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Aspekt ze ZBYT SZEROKIM punktem przecięcia (pułapka z sekcji 5). Pomysł brzmi rozsądnie: „zapisuj każde
 * wywołanie przestarzałej metody (oznaczonej {@code @Deprecated})”. Ale wyrażenie nie ma zawężenia do naszych
 * pakietów, więc Spring sprawdza je na KAŻDYM beanie — także na beanach samego Spring Boota. Jeden z nich ma metodę
 * {@code @Deprecated} i jest klasą final, której nie da się opakować w proxy CGLIB → aplikacja nie startuje.
 * Nie ma {@code @Component}: rejestruje go tylko test sekcji 5 (inaczej cała lekcja by nie wstała).
 */
@Aspect
public class BroadDeprecatedAspect {

    private final CallJournal journal;

    public BroadDeprecatedAspect(CallJournal journal) {
        this.journal = journal;
    }

    @Before("@annotation(java.lang.Deprecated)") // brak within(s18_aop..*) — łapie WSZYSTKIE beany
    public void reportDeprecatedCall(JoinPoint joinPoint) {
        journal.add("PRZESTARZAŁA metoda: " + joinPoint.getSignature().toShortString());
    }
}
