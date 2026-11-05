package s12_security.sec01_basics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfiguracja bezpieczeństwa lekcji Sec01Basics (sekcje 3–5).
 * <p>
 * {@code @Configuration} (konfiguracja) z dwoma beanami (ziarnami — obiektami zarządzanymi przez Springa):
 * <ul>
 *   <li>{@code SecurityFilterChain} (łańcuch filtrów bezpieczeństwa) — KTO może wejść POD KTÓRY adres i JAK się loguje;</li>
 *   <li>{@code UserDetailsService} (usługa danych użytkownika) — SKĄD wziąć użytkownika i jego hasło.</li>
 * </ul>
 * Sam fakt, że istnieje nasz bean {@code SecurityFilterChain}, wyłącza domyślny łańcuch Boota ({@code defaultSecurityFilterChain})
 * — test Sec01FilterChainTest.ownChainReplacesDefault.
 */
@Configuration
public class Sec01SecurityConfig {

    @Bean
    SecurityFilterChain sec01FilterChain(HttpSecurity http) {
        // HttpSecurity = budowniczy łańcucha filtrów. W Spring Security 7 TYLKO styl lambda (lambda DSL):
        // każda część konfiguracji dostaje własną lambdę, nie ma już .and() ani authorizeRequests().
        http
                .authorizeHttpRequests(auth -> auth          // authorize HTTP requests = autoryzuj żądania HTTP (reguły adresów)
                        .requestMatchers("/sec01/public/**").permitAll()       // permit all = wpuść każdego, także anonima
                        .requestMatchers("/error").permitAll()                 // strona błędów Boota — PUŁAPKA z sekcji 5
                        .requestMatchers("/sec01/admin/**").hasRole("ADMIN")   // has role = ma rolę (szczegóły: Sec02Authorization)
                        .anyRequest().authenticated())                         // any request = każde inne żądanie: tylko zalogowani
                .httpBasic(Customizer.withDefaults())     // HTTP Basic = login i hasło w nagłówku Authorization (dla API)
                .formLogin(Customizer.withDefaults());    // form login = formularz /login (dla przeglądarki)
        return http.build();
    }

    @Bean
    UserDetailsService sec01Users() {
        // InMemoryUserDetailsManager = menedżer użytkowników w pamięci — dobry do nauki i testów, nie na produkcję.
        // {noop} = "no operation", hasło zapisane JAWNIE. Tak NIE przechowuje się haseł — BCrypt pokazuje Sec03UsersPasswords.
        // Hasła TYLKO DO NAUKI.
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build(),
                User.withUsername("admin").password("{noop}haslo-testowe").roles("ADMIN").build());
    }
}
