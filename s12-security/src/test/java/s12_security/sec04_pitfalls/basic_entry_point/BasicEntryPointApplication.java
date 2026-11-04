package s12_security.sec04_pitfalls.basic_entry_point;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NIEPEŁNA konfiguracja (pułapka z sekcji 4 lekcji Sec04StatelessApi): własny punkt wejścia (JSON) ustawiony tylko
 * w {@code exceptionHandling}, a nie w {@code httpBasic}. Brak logowania obsługuje {@code ExceptionTranslationFilter} — nasz
 * JSON. ZŁE HASŁO obsługuje jednak sam {@code BasicAuthenticationFilter} swoim domyślnym punktem wejścia — bez JSON-a.
 */
@SpringBootApplication
public class BasicEntryPointApplication {

    @RestController
    static class OrdersController {

        @GetMapping("/sec04/pitfall/orders")
        public String orders() {
            return "Zamówienia";
        }
    }

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        AuthenticationEntryPoint json = (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"status\":401,\"title\":\"Wymagane logowanie\"}");
        };
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())                                 // brakuje: authenticationEntryPoint(json)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(json));
        return http.build();
    }

    @Bean
    UserDetailsService pitfallUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build());   // TYLKO DO NAUKI
    }
}
