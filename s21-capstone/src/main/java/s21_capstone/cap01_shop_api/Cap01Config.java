package s21_capstone.cap01_shop_api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Konfiguracja projektu Cap01ShopApi: zegar i JAWNE wyłączenie logowania.
 *
 * <p>DLACZEGO SecurityFilterChain z {@code permitAll()} w projekcie bez logowania? Starter Spring Security jest na
 * ścieżce klas CAŁEGO modułu s21-capstone (potrzebuje go projekt Cap03). Sama jego obecność włącza autokonfigurację
 * zabezpieczeń: bez tego beana każde zapytanie do API skończyłoby się 401 (brak logowania), a POST — 403
 * (brak tokenu CSRF). Własny łańcuch filtrów wyłącza domyślny łańcuch Boota (warunek „brak beana SecurityFilterChain”)
 * i mówi wprost: „to API jest publiczne”. Test Cap01ShopApiDefaultSecurityTest pokazuje, co by było bez niego (401/403).</p>
 *
 * <p>W prawdziwym sklepie dodawanie produktów i zmiana statusów wymagałyby roli (tak jak w Cap03LibraryWithAuth) —
 * tutaj świadomie skupiamy się na REST, JPA i błędach.</p>
 */
@Configuration
public class Cap01Config {

    /** Strefa czasowa sklepu — „teraz” liczymy w czasie polskim, także na serwerze ustawionym na UTC. */
    public static final ZoneId SHOP_ZONE = ZoneId.of("Europe/Warsaw");

    @Bean
    Clock clock() {
        return Clock.system(SHOP_ZONE);
    }

    @Bean
    SecurityFilterChain cap01SecurityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());   // permit all = wpuść wszystkich
        http.csrf(AbstractHttpConfigurer::disable);   // API bez ciasteczek sesji — CSRF nie ma tu czego chronić
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
