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
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Sec05JwtConfig = wszystko, czego potrzeba do JWT w lekcji Sec05Jwt:
 * <ol>
 *   <li>para kluczy RSA wygenerowana PRZY STARCIE (klucz testowy — w repozytorium nie ma żadnego pliku z kluczem),</li>
 *   <li>{@code JwtEncoder} (koder tokenów — podpisuje kluczem prywatnym) i {@code JwtDecoder} (dekoder — sprawdza
 *       podpis kluczem publicznym, ważność i wystawcę),</li>
 *   <li>dwa łańcuchy: {@code /api/auth/token} — logowanie hasłem (Basic) i wydanie tokenu; reszta — Resource Server
 *       (serwer zasobów), który wpuszcza tylko z ważnym tokenem w nagłówku {@code Authorization: Bearer ...}.</li>
 * </ol>
 */
@Configuration
public class Sec05JwtConfig {

    /**
     * Wystawca tokenów (claim „iss”) — dekoder odrzuci token z innym wystawcą. Zwykle to adres URL serwera tożsamości;
     * domena .example jest zarezerwowana do przykładów, więc nikt nie wyśle tam prawdziwych zapytań.
     */
    public static final String ISSUER = "https://auth.sklep.example";

    /** Zegar aplikacji: z niego TokenService bierze „teraz”, a dekoder sprawdza ważność tokenu. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Para kluczy RSA 2048 bitów — TESTOWA, nowa przy każdym starcie (więc tokeny sprzed restartu przestają działać).
     * W prawdziwym systemie klucz prywatny ma tylko serwer tożsamości, a aplikacja pobiera klucz publiczny (JWKS).
     */
    @Bean
    KeyPair jwtKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");   // key pair generator = generator pary kluczy
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    /** Koder: podpisuje tokeny kluczem PRYWATNYM, algorytm RS256 (RSA + SHA-256). */
    @Bean
    JwtEncoder jwtEncoder(KeyPair keyPair) {
        return NimbusJwtEncoder.withKeyPair((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate())
                .build();
    }

    /**
     * Dekoder: sprawdza podpis kluczem PUBLICZNYM i uruchamia walidatory: typ tokenu, czas (exp/nbf, z tolerancją
     * 60 s, liczony NASZYM zegarem) i wystawcę (iss).
     */
    @Bean
    JwtDecoder jwtDecoder(KeyPair keyPair, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ofSeconds(60));   // clock skew = tolerancja zegara
        timestamps.setClock(clock);
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestamps, new JwtIssuerValidator(ISSUER)));
        return decoder;
    }

    /**
     * Jak zamienić claimy tokenu na uprawnienia (GrantedAuthority):
     * claim „scope” (zakres, napis rozdzielany spacjami) → SCOPE_products:read, SCOPE_products:write (domyślnie);
     * claim „roles” (lista) → ROLE_ADMIN (nasz dodatek, żeby działało hasRole).
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();   // domyślnie: scope/scp → SCOPE_
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");   // authorities claim name = nazwa claimu z uprawnieniami
        roles.setAuthorityPrefix("ROLE_");        // authority prefix = prefiks uprawnienia
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new DelegatingJwtGrantedAuthoritiesConverter(scopes, roles));
        return converter;
    }

    /** Łańcuch 1: wymiana loginu i hasła (HTTP Basic) na token. Tylko POST /api/auth/token. */
    @Bean
    @Order(1)
    SecurityFilterChain tokenChain(HttpSecurity http) {
        http.securityMatcher("/api/auth/token");
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        http.httpBasic(Customizer.withDefaults());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);   // bez sesji i ciasteczek — Sec04StatelessApi, sekcja 3
        return http.build();
    }

    /** Łańcuch 2: Resource Server — każde inne zapytanie musi mieć ważny token JWT. */
    @Bean
    @Order(2)
    SecurityFilterChain resourceServerChain(HttpSecurity http, JwtAuthenticationConverter converter) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products").hasAuthority("SCOPE_products:read")
                .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority("SCOPE_products:write")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
        http.oauth2ResourceServer(resourceServer -> resourceServer   // resource server = serwer zasobów
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Konta (hasła TESTOWE): anna — ADMIN, piotr — USER. */
    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(List.of(
                User.withUsername("anna").password(encoder.encode("anna-test-123")).roles("ADMIN").build(),
                User.withUsername("piotr").password(encoder.encode("piotr-test-123")).roles("USER").build()));
    }
}
