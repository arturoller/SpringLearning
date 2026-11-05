package s12_security.sec04_stateless_api;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SZKIELET ĆWICZENIA 4 lekcji Sec04StatelessApi: bezstanowy łańcuch dla {@code /sec04/cw/**}.
 * Celowo BEZ {@code @Configuration} (inaczej aplikacja lekcji miałaby trzeci łańcuch). Test (Sec04ExerciseSecurityTest)
 * ładuje {@link ExerciseSecurityBeans}, które wołają tę metodę. Dopóki jest TODO, kontekst testu się nie uruchomi
 * (przyczyna: {@code UnsupportedOperationException: TODO}).
 */
public final class ExerciseSecurityConfig {

    private ExerciseSecurityConfig() {
    }

    /**
     * Ćwiczenie 4: łańcuch tylko dla {@code /sec04/cw/**}: {@code GET /sec04/cw/catalog} dla każdego, reszta dla zalogowanych;
     * bez sesji (STATELESS), CSRF wyłączony, CORS z {@link ExerciseCors#source()}, HTTP Basic; brak logowania i złe hasło
     * obsługuje {@link ExerciseJsonEntryPoint}. Zwróć {@code http.build()}.
     */
    public static SecurityFilterChain filterChain(HttpSecurity http) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
