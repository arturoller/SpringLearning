package s12_security.sec02_authorization;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SZKIELET ĆWICZENIA 4 lekcji Sec02Authorization: reguły adresów {@code /sec02/cw/**} w dobrej kolejności.
 * Celowo BEZ {@code @Configuration} (inaczej aplikacja lekcji miałaby drugi łańcuch). Test (Sec02ExerciseSecurityTest)
 * ładuje {@link ExerciseSecurityBeans}, które wołają tę metodę. Dopóki jest TODO, kontekst testu się nie uruchomi
 * (przyczyna: {@code UnsupportedOperationException: TODO}).
 */
public final class ExerciseSecurityConfig {

    private ExerciseSecurityConfig() {
    }

    /**
     * Ćwiczenie 4: {@code GET /sec02/cw/catalog/hidden/**} tylko EMPLOYEE; pozostałe {@code GET /sec02/cw/catalog/**} dla
     * każdego; {@code POST /sec02/cw/catalog} tylko EMPLOYEE; {@code /error} dla każdego; WSZYSTKO inne zamknięte (denyAll).
     * Logowanie HTTP Basic, CSRF wyłączony (jak w lekcji). Zwróć {@code http.build()}.
     */
    public static SecurityFilterChain filterChain(HttpSecurity http) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
