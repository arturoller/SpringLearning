package s12_security.sec02_authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Sec02Users = pracownicy sklepu w lekcji Sec02Authorization. Osobna klasa (a nie metoda w Sec02SecurityConfig),
 * żeby testy mogły dołączyć tych samych użytkowników do INNEGO, celowo błędnego łańcucha filtrów.
 */
@Configuration
public class Sec02Users {

    /**
     * Hasła TESTOWE; {@code {noop}} = jawny tekst — tylko w lekcji (porządnie: Sec03UsersPasswords).
     * <ul>
     *   <li>anna — rola ADMIN,</li>
     *   <li>piotr — rola USER,</li>
     *   <li>kasia — rola HR (dział kadr),</li>
     *   <li>tomasz — rola SALES (sprzedaż) i uprawnienie {@code report:read} (czytanie raportów).</li>
     * </ul>
     * PUŁAPKA: {@code roles(...)} i {@code authorities(...)} w budowniczym User NADPISUJĄ się nawzajem (wygrywa ostatnie
     * wywołanie). Dlatego tomasz ma oba uprawnienia wpisane w JEDNYM {@code authorities(...)}, z ręcznym prefiksem ROLE_.
     */
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}anna-test-123").roles("ADMIN").build(),
                User.withUsername("piotr").password("{noop}piotr-test-123").roles("USER").build(),
                User.withUsername("kasia").password("{noop}kasia-test-123").roles("HR").build(),
                User.withUsername("tomasz").password("{noop}tomasz-test-123").authorities("ROLE_SALES", "report:read").build());
    }
}
