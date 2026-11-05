package s12_security.sec01_basics;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SZKIELET ĆWICZENIA 5 lekcji Sec01Basics: własny łańcuch filtrów dla adresów {@code /sec01/cw/**}.
 * <p>
 * Celowo BEZ {@code @Configuration}: gdyby to był bean konfiguracji, aplikacja lekcji miałaby DWA łańcuchy łapiące te same
 * adresy (i błąd przy starcie). Test ćwiczenia (Sec01ExerciseSecurityTest) podaje tę klasę w {@code @ContextConfiguration}
 * razem z {@link ExerciseSecurityBeans}, które wołają metodę {@link #filterChain}.
 * Dopóki jest TODO, kontekst testu się nie uruchomi — w błędzie zobaczysz {@code UnsupportedOperationException: TODO}.
 */
public final class ExerciseSecurityConfig {

    private ExerciseSecurityConfig() {
    }

    /**
     * Ćwiczenie 5: zbuduj łańcuch: {@code /sec01/cw/public/**} i {@code /error} dla każdego, {@code /sec01/cw/admin/**} tylko
     * dla roli ADMIN, reszta tylko dla zalogowanych; logowanie HTTP Basic. Zwróć {@code http.build()}.
     */
    public static SecurityFilterChain filterChain(HttpSecurity http) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
