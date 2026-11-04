package s12_security.sec05_jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;

/**
 * Konfiguracja lekcji Sec05Jwt: wydawanie tokenów (hasło → token) i API, które przyjmuje tylko tokeny.
 * <ol>
 *   <li>{@link #tokenChain} ({@code @Order(1)}, tylko {@code POST /sec05/token}) — logowanie HTTP Basic, wynik: token JWT.</li>
 *   <li>{@link #apiChain} ({@code @Order(2)}, wszystko inne) — Resource Server: {@code Authorization: Bearer <token>}.</li>
 * </ol>
 * Oba łańcuchy są bezstanowe i bez CSRF (Sec04StatelessApi: poświadczenia tylko w nagłówku, żadnych ciasteczek).
 */
@Configuration
public class Sec05SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain tokenChain(HttpSecurity http) {
        http
                .securityMatcher("/sec05/token")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())          // TU, i tylko tu, przychodzi hasło
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationConverter rolesConverter) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/sec05/products").permitAll()
                        .requestMatchers("/sec05/orders/**", "/sec05/me").authenticated()
                        .requestMatchers("/sec05/admin/**").hasRole("ADMIN")      // rola z twierdzenia "roles" (sekcja 4)
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                // resource server = serwer zasobów: przyjmuje tokeny wydane przez kogoś (tu: przez nas samych).
                // BearerTokenAuthenticationFilter czyta nagłówek, JwtDecoder sprawdza podpis i daty, konwerter robi role.
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Klucz HMAC (wspólny sekret) do podpisywania i sprawdzania tokenów — TYLKO DO NAUKI: losowany przy starcie, więc nie ma go
     * w repozytorium, a restart unieważnia stare tokeny. Na produkcji: klucz z menedżera sekretów / zmiennej środowiska
     * (co najmniej 256 bitów dla HS256) albo para kluczy RSA i serwer autoryzacji (sekcja 5).
     */
    @Bean
    SecretKey jwtSecretKey() throws NoSuchAlgorithmException {
        return KeyGenerator.getInstance("HmacSHA256").generateKey();    // 256 losowych bitów
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).build();
    }

    /**
     * Dekoder sprawdza podpis (tym samym kluczem) i daty ({@code exp}, {@code nbf}). Walidator dat dostaje NASZ {@link Clock} —
     * ten sam co {@link TokenService} — więc testy mogą sterować czasem.
     */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator();   // domyślnie 60 s tolerancji na różnice zegarów
        timestamps.setClock(clock);
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestamps));
        return decoder;
    }

    /** Twierdzenie {@code roles: ["ADMIN"]} → uprawnienie {@code ROLE_ADMIN} (sekcja 4). */
    @Bean
    JwtAuthenticationConverter rolesConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");     // zamiast domyślnego "scope" / "scp"
        roles.setAuthorityPrefix("ROLE_");          // zamiast domyślnego "SCOPE_"
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    UserDetailsService sec05Users() {
        // Hasła TYLKO DO NAUKI, zapisane jawnie ({noop}) — porządne hashowanie: Sec03UsersPasswords.
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build(),
                User.withUsername("ewa").password("{noop}haslo-testowe").roles("EMPLOYEE").build(),
                User.withUsername("admin").password("{noop}haslo-testowe").roles("ADMIN").build());
    }
}
