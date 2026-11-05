package s12_security.sec03_users_passwords;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfiguracja bezpieczeństwa lekcji Sec03UsersPasswords. Zauważ, czego tu NIE MA: żadnego {@code UserDetailsService} ani
 * listy użytkowników — użytkowników daje {@link JpaUserDetailsService} (bean z bazy), a Spring Security sam go znajduje.
 */
@Configuration
public class Sec03SecurityConfig {

    @Bean
    SecurityFilterChain sec03FilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/sec03/register").permitAll()   // rejestracja: bez logowania
                        .requestMatchers("/sec03/me/**", "/sec03/me").authenticated()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                // Jak w Sec02Authorization: API wołają programy, nie przeglądarka — kiedy wolno wyłączyć CSRF: Sec04StatelessApi.
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * {@code DelegatingPasswordEncoder} (koder delegujący): NOWE hasła koduje BCryptem i zapisuje z prefiksem {@code {bcrypt}},
     * a przy sprawdzaniu czyta prefiks i wybiera właściwy algorytm ({@code {noop}}, {@code {pbkdf2}}, {@code {argon2}}…).
     * Dzięki temu w jednej bazie mogą żyć stare i nowe hashe (sekcja 2).
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
