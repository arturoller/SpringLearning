package s18_aop.aop03_custom_annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * CELOWO ZEPSUTA adnotacja (pułapka z sekcji 1): brak {@code @Retention(RetentionPolicy.RUNTIME)}.
 * Domyślne przechowywanie to CLASS — adnotacja trafia do pliku .class, ale Java NIE udostępnia jej w działającym
 * programie (refleksja jej nie widzi). Porada AuditAspect.brokenAudit, która chce odczytać jej atrybut, nigdy się
 * nie wykonuje — bez błędu i bez ostrzeżenia.
 */
@Target(ElementType.METHOD)
public @interface BrokenAuditLog {

    String value();
}
