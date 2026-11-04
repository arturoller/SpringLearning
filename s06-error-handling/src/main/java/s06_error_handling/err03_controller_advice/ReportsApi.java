package s06_error_handling.err03_controller_advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Własna adnotacja-znacznik (marker annotation = adnotacja bez pól, która tylko "oznacza" klasę). Kontrolery z tą adnotacją
 * obsługuje {@code ReportsAdvice} (selektor {@code annotations = ReportsApi.class}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReportsApi {
}
