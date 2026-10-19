package s12_security.sec02_authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sec02SecurityConfig = reguły dostępu lekcji Sec02Authorization (użytkownicy — w Sec02Users).
 *
 * <p>{@code @EnableMethodSecurity} (włącz zabezpieczenia metod) — bez tej adnotacji {@code @PreAuthorize} na metodach
 * OrderService byłoby ZWYKŁĄ adnotacją bez żadnego działania (sekcja 4 lekcji).</p>
 */
@Configuration
@EnableMethodSecurity
public class Sec02SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                // od SZCZEGÓŁU do OGÓŁU — pierwsza pasująca reguła wygrywa
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()          // czytać może każdy
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")       // dodawać — tylko ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")  // usuwać — tylko ADMIN
                .requestMatchers("/api/hr/**").hasAnyRole("HR", "ADMIN")                   // has any role = ma którąkolwiek z ról
                .requestMatchers("/api/reports/**").hasAuthority("report:read")           // has authority = ma uprawnienie (bez prefiksu!)
                .requestMatchers("/api/orders/**").authenticated()                        // szczegóły sprawdza @PreAuthorize w OrderService
                .anyRequest().denyAll());                                                 // deny all = zabroń wszystkim: czego nie wymieniono — nie wolno
        http.httpBasic(Customizer.withDefaults());
        // CSRF wyłączone, bo to API dla programów, które w KAŻDYM zapytaniu wysyłają login i hasło (Basic), a nie dla
        // przeglądarki z ciasteczkiem sesji. Kiedy wolno tak zrobić, a kiedy NIE — Sec04StatelessApi. Tutaj chodzi o to,
        // żeby POST i DELETE sprawdzały tylko reguły ról (z włączonym CSRF POST bez tokenu dostałby 403 nawet od admina).
        http.csrf(AbstractHttpConfigurer::disable);   // disable = wyłącz
        return http.build();
    }
}
