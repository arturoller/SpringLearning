package s21_capstone.cap03_library_with_auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import s21_capstone.cap03_library_with_auth.auth.TokenService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;

/**
 * Zabezpieczenia biblioteki (s12_security/Sec04StatelessApi, s12_security/Sec05Jwt w jednym miejscu):
 * <ol>
 *   <li>hasła: {@code DelegatingPasswordEncoder} — skróty {bcrypt}... w tabeli library_users;</li>
 *   <li>logowanie: {@code AuthenticationManager} = DaoAuthenticationProvider(UserDetailsService z bazy + BCrypt),
 *       używany w AuthController (POST /api/auth/login);</li>
 *   <li>tokeny: para kluczy RSA generowana PRZY STARCIE (klucz testowy — w repozytorium nie ma żadnego klucza),
 *       JwtEncoder (podpis) i JwtDecoder (sprawdzenie podpisu, ważności NASZYM zegarem i wystawcy);</li>
 *   <li>claim „roles” → uprawnienia ROLE_..., więc działa {@code hasRole('BIBLIOTEKARZ')};</li>
 *   <li>jeden łańcuch filtrów: Resource Server (serwer zasobów) bez sesji i bez CSRF + reguły URL;</li>
 *   <li>{@code @EnableMethodSecurity} — {@code @PreAuthorize}/{@code @PostAuthorize} w serwisach.</li>
 * </ol>
 */
@Configuration
@EnableMethodSecurity
public class Cap03SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Menedżer uwierzytelniania (authentication manager) dla punktu logowania: login + hasło → Authentication. */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /** Para kluczy RSA 2048 bitów — TESTOWA, nowa przy każdym starcie (tokeny sprzed restartu przestają działać). */
    @Bean
    KeyPair jwtKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Bean
    JwtEncoder jwtEncoder(KeyPair keyPair) {
        return NimbusJwtEncoder.withKeyPair((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate())
                .build();
    }

    /** Dekoder: podpis kluczem publicznym; ważność (exp, tolerancja 60 s) liczona TYM SAMYM zegarem co wystawianie. */
    @Bean
    JwtDecoder jwtDecoder(KeyPair keyPair, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ofSeconds(60));
        timestamps.setClock(clock);
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(timestamps,
                new JwtIssuerValidator(TokenService.ISSUER)));
        return decoder;
    }

    /** claim „roles” (lista) → ROLE_BIBLIOTEKARZ / ROLE_CZYTELNIK. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /**
     * Reguły URL — od najbardziej szczegółowych, na końcu „reszta wymaga logowania”. Publiczne: logowanie i PRZEGLĄDANIE
     * katalogu. Zmiany katalogu i obsługa wypożyczeń — tylko BIBLIOTEKARZ. Dostęp do konkretnych wypożyczeń
     * (czyje?) rozstrzygają adnotacje w LoanService, bo zależy od DANYCH, nie od adresu.
     */
    @Bean
    SecurityFilterChain cap03SecurityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/*").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/books").hasRole("BIBLIOTEKARZ")
                .requestMatchers(HttpMethod.POST, "/api/loans", "/api/loans/*/return").hasRole("BIBLIOTEKARZ")
                .anyRequest().authenticated());
        http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);   // token w nagłówku, nie w ciasteczku — CSRF nie dotyczy
        return http.build();
    }
}
