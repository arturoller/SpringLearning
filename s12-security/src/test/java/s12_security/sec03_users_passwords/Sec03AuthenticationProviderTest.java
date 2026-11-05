package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 4 i 5 lekcji Sec03UsersPasswords: co dokładnie robi {@code DaoAuthenticationProvider} — ten sam obiekt, który
 * Spring Security buduje z naszego UserDetailsService i PasswordEncoder. Składamy go ręcznie, bez Springa, żeby zobaczyć
 * wyjątki, których klient HTTP nigdy nie widzi (on dostaje tylko 401).
 */
class Sec03AuthenticationProviderTest {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private DaoAuthenticationProvider provider() {
        String hash = encoder.encode("haslo-testowe");
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("anna").password(hash).roles("CUSTOMER").build(),
                User.withUsername("jan").password(hash).roles("CUSTOMER").disabled(true).build());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);   // Security 7: UserDetailsService w konstruktorze
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    private static UsernamePasswordAuthenticationToken login(String username, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(username, password);
    }

    @Test
    @DisplayName("Sekcja 4: poprawne dane → uwierzytelniony token z rolą i z uprawnieniem FACTOR_PASSWORD")
    void success() {
        Authentication result = provider().authenticate(login("anna", "haslo-testowe"));
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities()).extracting(Object::toString).contains("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Sekcja 5: domyślnie (hideUserNotFoundExceptions = true) nieznany login i złe hasło to TEN SAM wyjątek")
    void sameExceptionForUnknownUserAndWrongPassword() {
        DaoAuthenticationProvider provider = provider();
        assertThat(provider.isHideUserNotFoundExceptions()).isTrue();
        assertThatThrownBy(() -> provider.authenticate(login("anna", "zle-haslo")))
                .isExactlyInstanceOf(BadCredentialsException.class).hasMessage("Bad credentials");
        assertThatThrownBy(() -> provider.authenticate(login("nikt", "zle-haslo")))
                .isExactlyInstanceOf(BadCredentialsException.class).hasMessage("Bad credentials");
    }

    @Test
    @DisplayName("Sekcja 5 (pułapka): hideUserNotFoundExceptions = false zdradza, że konta NIE MA (UsernameNotFoundException)")
    void revealingUserNotFound() {
        DaoAuthenticationProvider provider = provider();
        provider.setHideUserNotFoundExceptions(false);
        assertThatThrownBy(() -> provider.authenticate(login("nikt", "zle-haslo")))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Sekcja 4: konto wyłączone → DisabledException — nawet przy ZŁYM haśle (stan konta sprawdzany przed hasłem)")
    void disabledAccount() {
        DaoAuthenticationProvider provider = provider();
        assertThatThrownBy(() -> provider.authenticate(login("jan", "haslo-testowe"))).isInstanceOf(DisabledException.class);
        assertThatThrownBy(() -> provider.authenticate(login("jan", "zle-haslo"))).isInstanceOf(DisabledException.class);
    }
}
