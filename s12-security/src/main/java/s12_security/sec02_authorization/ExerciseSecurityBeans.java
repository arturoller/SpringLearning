package s12_security.sec02_authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Gotowa "obudowa" ćwiczenia 4: zamienia {@link ExerciseSecurityConfig#filterChain} w bean. Bez {@code @Configuration}, więc
 * skanowanie lekcji jej nie znajduje — ładuje ją tylko test ćwiczenia ({@code @ContextConfiguration}).
 */
@EnableWebSecurity
public class ExerciseSecurityBeans {

    @Bean
    SecurityFilterChain exerciseFilterChain(HttpSecurity http) {
        return ExerciseSecurityConfig.filterChain(http);
    }
}
