package s18_aop.aop03_custom_annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adnotacja do ćwiczenia 3: „ponów przy chwilowej awarii” (retry on failure). Ponawia tylko po
 * TemporaryFailureException. W prawdziwym projekcie użyj wbudowanego {@code @Retryable} ze Spring Framework 7
 * (s13_rest_client/Rc04Resilience) — tu piszemy uproszczoną wersję, żeby zobaczyć, jak działa w środku.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryOnFailure {

    /** Ile razy w sumie wolno wywołać metodę (pierwsza próba + ponowienia). */
    int attempts() default 3;
}
