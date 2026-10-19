package s18_aop.aop03_custom_annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Własna adnotacja „zapisz w dzienniku audytu” (audit log = dziennik audytu: kto, co i kiedy zrobił).
 * Sama nic nie robi — to ZNACZNIK. Zachowanie dokłada AuditAspect ({@code @Around("@annotation(auditLog)")}).
 * <ul>
 *   <li>{@code @Target(METHOD)} (target = cel) — wolno ją postawić tylko na metodzie; na klasie kompilator zgłosi błąd.</li>
 *   <li>{@code @Retention(RUNTIME)} (retention = przechowywanie) — adnotacja jest widoczna w działającym programie.
 *       Bez tego aspekt z dostępem do atrybutów jej nie zobaczy (pułapka z sekcji 1, BrokenAuditLog).</li>
 *   <li>{@code @Documented} — adnotacja pojawi się w dokumentacji Javadoc oznaczonej metody.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** Nazwa operacji w dzienniku, np. „ZMIANA_CENY”. Brak wartości domyślnej = atrybut obowiązkowy. */
    String action();
}
