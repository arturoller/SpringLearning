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
 * Sec01SecurityConfig = NASZA konfiguracja bezpieczeństwa lekcji Sec01Basics (sekcja 3).
 *
 * <p>{@code @Configuration} (konfiguracja) — klasa z metodami {@code @Bean}. Gdy w kontekście pojawi się choć jeden
 * bean typu {@code SecurityFilterChain} (łańcuch filtrów bezpieczeństwa), Boot WYCOFUJE swój domyślny łańcuch
 * (warunek {@code @ConditionalOnMissingBean(SecurityFilterChain.class)} w ServletWebSecurityAutoConfiguration).
 * Od tej chwili obowiązuje tylko to, co napiszesz tutaj — także formularz logowania znika, jeśli go nie dodasz.</p>
 */
@Configuration
public class Sec01SecurityConfig {

    /**
     * Łańcuch filtrów — DSL z lambdami (DSL = domain-specific language, mały „język” do konfiguracji).
     * {@code HttpSecurity} (zabezpieczenia HTTP) to budowniczy łańcucha; Spring wstrzykuje go do metody {@code @Bean}.
     * W Security 7 {@code build()} nie rzuca już wyjątku sprawdzanego — nie trzeba pisać {@code throws Exception}.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth                       // authorize = zezwalaj (reguły dostępu do adresów)
                .requestMatchers("/api/public/**").permitAll()       // request matcher = dopasowanie zapytania; permit all = wpuść każdego
                .requestMatchers("/error").permitAll()               // strona błędu Boota — dlaczego: Sec01Basics, sekcja 4
                .requestMatchers("/api/admin/**").hasRole("ADMIN")   // has role = ma rolę (szczegóły: Sec02Authorization)
                .anyRequest().authenticated());                      // any request = każde inne zapytanie; authenticated = zalogowany
        http.httpBasic(Customizer.withDefaults());   // HTTP Basic: login i hasło w nagłówku Authorization; with defaults = z ustawieniami domyślnymi
        return http.build();                         // build = zbuduj łańcuch
    }

    /**
     * Użytkownicy lekcji, trzymani w pamięci. {@code UserDetailsService} (usługa danych użytkownika) to interfejs,
     * z którego Spring Security pyta „kim jest anna i jakie ma hasło?”. Gdy go zdefiniujesz, Boot nie tworzy
     * użytkownika {@code user} z wygenerowanym hasłem.
     *
     * <p>PUŁAPKA: {@code {noop}} (no operation — bez zmian) oznacza hasło zapisane JAWNYM tekstem. Tu tylko dlatego,
     * żeby pierwsza lekcja była krótka; hasła są testowe. Jak przechowywać hasła naprawdę (BCrypt) — Sec03UsersPasswords.</p>
     */
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(                              // in-memory = w pamięci
                User.withUsername("anna").password("{noop}anna-test-123").roles("ADMIN").build(),
                User.withUsername("piotr").password("{noop}piotr-test-123").roles("USER").build());
    }
}
