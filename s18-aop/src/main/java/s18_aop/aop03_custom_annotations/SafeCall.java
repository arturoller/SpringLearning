package s18_aop.aop03_custom_annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CELOWO ZŁY pomysł (pułapka z sekcji 4): „bezpieczne wywołanie” — metoda oznaczona {@code @SafeCall} nigdy nie
 * rzuca wyjątku, bo SafeCallAspect łapie go i zwraca null. Brzmi wygodnie, a ukrywa błędy.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeCall {
}
