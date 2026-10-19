package s18_aop.aop03_custom_annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Własna adnotacja „zmierz czas wykonania” (measure time = zmierz czas). Można ją postawić na METODZIE albo na
 * KLASIE (wtedy dotyczy wszystkich metod klasy) — MeasureTimeAspect obsługuje oba przypadki, a adnotacja na metodzie
 * wygrywa z adnotacją na klasie.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MeasureTime {

    /** Próg w milisekundach: dłuższe wykonanie aspekt oznacza jako „WOLNO”. Wartość domyślna: 500. */
    long warnAboveMillis() default 500;
}
