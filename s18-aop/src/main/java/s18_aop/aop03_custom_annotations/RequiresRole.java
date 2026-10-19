package s18_aop.aop03_custom_annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adnotacja do ćwiczenia 1: „metoda wymaga roli” (requires role = wymaga roli). Sprawdzanie dokłada aspekt
 * z ćwiczenia (Aop03Exercise1RoleAspect). W prawdziwej aplikacji uprawnienia sprawdza Spring Security
 * ({@code @PreAuthorize} — s12_security/Sec02Authorization); tu ćwiczymy sam mechanizm aspektu.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /** Wymagana rola, np. „KIEROWNIK”. Atrybut o nazwie value można podać bez nazwy: {@code @RequiresRole("KIEROWNIK")}. */
    String value();
}
