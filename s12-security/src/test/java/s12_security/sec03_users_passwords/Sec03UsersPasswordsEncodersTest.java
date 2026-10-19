package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy sekcji 2–4 lekcji Sec03UsersPasswords — kodery haseł i InMemoryUserDetailsManager. Zwykłe testy jednostkowe:
 * kodery to zwykłe obiekty Javy, kontekst Springa nie jest potrzebny.
 */
class Sec03UsersPasswordsEncodersTest {

    @Test
    @DisplayName("2. InMemoryUserDetailsManager to też menedżer: tworzy, zmienia i usuwa użytkowników w pamięci")
    void inMemoryManager() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}anna-test-123").roles("ADMIN").build());
        manager.createUser(User.withUsername("ewa").password("{noop}ewa-test-123").roles("USER").build());

        assertThat(manager.userExists("ewa")).isTrue();
        manager.deleteUser("ewa");
        assertThat(manager.userExists("ewa")).isFalse();
    }

    @Test
    @DisplayName("2. Po udanym logowaniu hasło {noop} jest samo przekodowane na {bcrypt} (UserDetailsPasswordService)")
    void passwordIsUpgradedAfterLogin() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager(
                User.withUsername("ola").password("{noop}ola-test-123").roles("USER").build());
        // tak samo składa to Spring Security, gdy bean UserDetailsService jest też UserDetailsPasswordService
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(manager);
        provider.setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
        provider.setUserDetailsPasswordService(manager);

        provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("ola", "ola-test-123"));

        assertThat(manager.loadUserByUsername("ola").getPassword()).startsWith("{bcrypt}$2a$10$");
    }

    @Test
    @DisplayName("3. BCrypt: ten sam tekst → różne skróty (sól), oba pasują; format $2a$10$..., 60 znaków")
    void bcryptSaltsEveryHash() {
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();   // domyślny koszt 10

        String first = bcrypt.encode("anna-test-123");
        String second = bcrypt.encode("anna-test-123");

        assertThat(first).isNotEqualTo(second);
        assertThat(bcrypt.matches("anna-test-123", first)).isTrue();
        assertThat(bcrypt.matches("anna-test-123", second)).isTrue();
        assertThat(bcrypt.matches("zle-haslo", first)).isFalse();
        assertThat(first).startsWith("$2a$10$").hasSize(60);
    }

    @Test
    @DisplayName("3. Koszt jest zapisany w skrócie: hash z kosztem 12 sprawdzi koder z kosztem domyślnym")
    void costIsStoredInTheHash() {
        String strong = new BCryptPasswordEncoder(12).encode("piotr-test-123");

        assertThat(strong).startsWith("$2a$12$");
        assertThat(new BCryptPasswordEncoder().matches("piotr-test-123", strong)).isTrue();
    }

    @Test
    @DisplayName("4. DelegatingPasswordEncoder: koduje {bcrypt}, sprawdza wg prefiksu, {noop} = jawny tekst")
    void delegatingEncoder() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        String hash = encoder.encode("anna-test-123");
        assertThat(hash).startsWith("{bcrypt}$2a$10$");
        assertThat(encoder.matches("anna-test-123", hash)).isTrue();
        assertThat(encoder.matches("anna-test-123", "{noop}anna-test-123")).isTrue();

        // upgrade encoding = czy warto przekodować (stary/słaby format)?
        assertThat(encoder.upgradeEncoding("{noop}anna-test-123")).isTrue();
        assertThat(encoder.upgradeEncoding(hash)).isFalse();
    }

    @Test
    @DisplayName("4. PUŁAPKA: skrót BEZ prefiksu albo z nieznanym prefiksem → IllegalArgumentException przy sprawdzaniu")
    void hashWithoutPrefix() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String bareHash = new BCryptPasswordEncoder().encode("anna-test-123");   // $2a$10$... bez {bcrypt}

        assertThatThrownBy(() -> encoder.matches("anna-test-123", bareHash))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Given that there is no default password encoder configured, each password must have a "
                        + "password encoding prefix. Please either prefix this password with '{noop}' or set a default "
                        + "password encoder in `DelegatingPasswordEncoder`.");
        assertThatThrownBy(() -> encoder.matches("anna-test-123", "{sha512}abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("There is no password encoder mapped for the id 'sha512'. "
                        + "Check your configuration to ensure it matches one of the registered encoders.");
    }
}
