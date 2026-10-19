package s12_security.sec06_security_testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ćwiczenie 4 (trudniejsze) lekcji Sec06SecurityTesting — WŁASNA adnotacja testowa „zalogowany administrator”.
 *
 * <p>Uzupełnij ją tak, żeby oznaczała: użytkownik „anna”, rola ADMIN. Wystarczy META-ADNOTACJA (adnotacja na
 * adnotacji): Spring Security szuka {@code @WithMockUser} także na adnotacjach, którymi oznaczono test.
 * Test Sec06SecurityTestingExercisesTest.exercise4 jest gotowy — przejdzie, gdy adnotacja zacznie działać.
 * Podpowiedź: t19_annotations_reflection/Annotations02Custom (własne adnotacje, Retention, Target).</p>
 *
 * <p>Ta klasa leży w src/test, bo {@code @WithMockUser} pochodzi z biblioteki testowej (spring-security-test) — kod
 * w src/main jej nie widzi.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
// TODO: twoje rozwiązanie — dodaj tu meta-adnotację @WithMockUser(...) z użytkownikiem „anna” i rolą ADMIN
public @interface WithMockAdmin {
}
