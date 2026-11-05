package s12_security.sec01_basics;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Gotowa "obudowa" ćwiczenia 5: zamienia Twoją metodę {@link ExerciseSecurityConfig#filterChain} w bean. Nie ma adnotacji
 * {@code @Configuration}, więc skanowanie lekcji jej NIE znajduje — ładuje ją tylko test ćwiczenia ({@code @ContextConfiguration}).
 * {@code @EnableWebSecurity} (włącz bezpieczeństwo web) daje bean {@code HttpSecurity} także w kontekście zbudowanym ręcznie.
 */
@EnableWebSecurity
public class ExerciseSecurityBeans {

    @Bean
    SecurityFilterChain exerciseFilterChain(HttpSecurity http) {
        return ExerciseSecurityConfig.filterChain(http);
    }
}
