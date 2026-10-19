package s21_capstone.cap02_reservations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Konfiguracja projektu Cap02Reservations: zegar i jawne wyłączenie logowania (dlaczego — Cap01Config i sekcja 7
 * lekcji Cap01ShopApi: starter Security jest na ścieżce klas całego modułu).
 */
@Configuration
public class Cap02Config {

    /** Strefa czasowa serwisu — godziny pracy 9–17 to czas POLSKI, także na serwerze w UTC. */
    public static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    /**
     * Zegar aplikacji. Gdy ustawiona jest właściwość {@code cap02.clock.fixed} (np. 2026-10-19T10:00), zegar jest
     * ZATRZYMANY w tej chwili (Clock.fixed) — tak działa profil demonstracyjny cap02, dzięki czemu dane startowe
     * i scenariusz .http „pasują” zawsze. Bez właściwości — prawdziwy czas (Clock.system).
     * Cała aplikacja (reguły, zdarzenia, testy) bierze „teraz” z TEGO beana, nigdy z {@code LocalDateTime.now()}.
     */
    @Bean
    Clock clock(@Value("${cap02.clock.fixed:}") String fixed) {
        if (fixed.isBlank()) {
            return Clock.system(ZONE);
        }
        return Clock.fixed(LocalDateTime.parse(fixed).atZone(ZONE).toInstant(), ZONE);
    }

    @Bean
    SecurityFilterChain cap02SecurityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
