package s12_security.solutions.sec03_users_passwords;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import s12_security.sec03_users_passwords.EmployeeAccount;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Sec03UsersPasswordsExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
class Sec03UsersPasswordsSolutionsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Wzorzec 1: rozpoznawanie skrótu BCrypt")
    void solution1() {
        assertThat(Sec03UsersPasswordsSolutions.solution1("$2a$10$Aydxj54Ew6VDNf0dAofOt.EOnHbAGMd.wz6e9YfI3HXtrzBLLG1Nm")).isTrue();
        assertThat(Sec03UsersPasswordsSolutions.solution1("$2b$12$Aydxj54Ew6VDNf0dAofOt.EOnHbAGMd.wz6e9YfI3HXtrzBLLG1Nm")).isTrue();
        assertThat(Sec03UsersPasswordsSolutions.solution1("{bcrypt}$2a$10$Aydxj54Ew6VDNf0dAofOt.EOnHbAGMd.wz6e9YfI3HXtrzBLLG1Nm"))
                .as("z prefiksem to już format DelegatingPasswordEncoder").isFalse();
        assertThat(Sec03UsersPasswordsSolutions.solution1("$2a$10$zaKrotki")).isFalse();
        assertThat(Sec03UsersPasswordsSolutions.solution1("anna-test-123")).isFalse();
        assertThat(Sec03UsersPasswordsSolutions.solution1(null)).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: konto z „bazy” → UserDetails z rolami i blokadą")
    void solution2() {
        UserDetails ewa = Sec03UsersPasswordsSolutions.solution2(new EmployeeAccount("ewa", "{noop}x", List.of("USER", "HR"), false));
        assertThat(ewa.getUsername()).isEqualTo("ewa");
        assertThat(ewa.getPassword()).isEqualTo("{noop}x");
        assertThat(ewa.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_HR");
        assertThat(ewa.isEnabled()).isFalse();

        UserDetails anna = Sec03UsersPasswordsSolutions.solution2(new EmployeeAccount("anna", "{noop}y", List.of("ADMIN"), true));
        assertThat(anna.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Wzorzec 3: UserDetailsService na mapie, login bez względu na wielkość liter, logowanie BCryptem")
    void solution3() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        Map<String, EmployeeAccount> accounts = Map.of(
                "anna", new EmployeeAccount("anna", encoder.encode("anna-test-123"), List.of("ADMIN"), true));
        UserDetailsService service = Sec03UsersPasswordsSolutions.solution3(accounts);

        assertThat(service.loadUserByUsername("ANNA").getUsername()).isEqualTo("anna");
        assertThatThrownBy(() -> service.loadUserByUsername("nieznany")).isInstanceOf(UsernameNotFoundException.class);

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(encoder);
        assertThat(provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("Anna", "anna-test-123"))
                .getName()).isEqualTo("anna");
    }

    @Test
    @DisplayName("Wzorzec 4: login z SecurityContextHolder albo „anonim”")
    void solution4() {
        assertThat(Sec03UsersPasswordsSolutions.solution4()).as("pusty kontekst").isEqualTo("anonim");

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "klucz", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(Sec03UsersPasswordsSolutions.solution4()).as("anonim z AnonymousAuthenticationFilter").isEqualTo("anonim");

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated("piotr", "x"));
        assertThat(Sec03UsersPasswordsSolutions.solution4()).as("nieuwierzytelniony bilet").isEqualTo("anonim");

        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "piotr", null, AuthorityUtils.createAuthorityList("ROLE_USER")));
        assertThat(Sec03UsersPasswordsSolutions.solution4()).isEqualTo("piotr");
    }
}
