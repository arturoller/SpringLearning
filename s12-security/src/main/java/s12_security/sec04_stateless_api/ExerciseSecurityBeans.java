package s12_security.sec04_stateless_api;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Gotowa "obudowa" ćwiczenia 4: zamienia {@link ExerciseSecurityConfig#filterChain} w bean i dodaje użytkowniczkę anna.
 * Bez {@code @Configuration}, więc skanowanie lekcji jej nie znajduje — ładuje ją tylko test ćwiczenia
 * ({@code @ContextConfiguration}).
 */
@EnableWebSecurity
public class ExerciseSecurityBeans {

    @Bean
    SecurityFilterChain exerciseFilterChain(HttpSecurity http) {
        return ExerciseSecurityConfig.filterChain(http);
    }

    @Bean
    UserDetailsService exerciseUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build());   // TYLKO DO NAUKI
    }
}
