package s12_security.sec04_stateless_api;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Pomocnik testu Sec04SpaCsrfTest (sekcja 2): łańcuch z ochroną CSRF w wersji dla SPA — {@code csrf(c -> c.spa())}.
 * Celowo bez {@code @Configuration} (tylko {@code @EnableWebSecurity}, które nie jest konfiguracją skanowaną): klasy testowe
 * leżą w pakiecie {@code Sec04Application}, więc testy {@code @SpringBootTest} lekcji zeskanowałyby taką klasę.
 */
@EnableWebSecurity
class SpaCsrfConfig {

    @Bean
    SecurityFilterChain spaChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                // spa() = token w ciasteczku XSRF-TOKEN (czytelnym dla JavaScriptu), odsyłany w nagłówku X-XSRF-TOKEN
                .csrf(csrf -> csrf.spa());
        return http.build();
    }

    @Bean
    UserDetailsService spaUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build());   // TYLKO DO NAUKI
    }
}
