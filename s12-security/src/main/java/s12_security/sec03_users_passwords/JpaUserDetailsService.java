package s12_security.sec03_users_passwords;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Most między bazą a Spring Security (sekcje 1 i 4 lekcji Sec03UsersPasswords).
 * <ul>
 *   <li>{@code UserDetailsService} (usługa danych użytkownika) — Spring Security pyta: "daj mi użytkownika o tej nazwie";
 *       my czytamy encję {@link AppUser} i zamieniamy ją na {@code UserDetails}. Hasła NIE sprawdzamy — robi to
 *       {@code DaoAuthenticationProvider} przy pomocy {@code PasswordEncoder}.</li>
 *   <li>{@code UserDetailsPasswordService} (usługa zmiany hasła) — Spring Security woła {@link #updatePassword}, gdy po udanym
 *       logowaniu uzna, że hash jest przestarzały (np. {@code {noop}} albo za niski koszt BCrypt), i podaje NOWY hash.</li>
 * </ul>
 * Wystarczy, że ten bean istnieje: Spring Security sam podłącza go do logowania (jedyny UserDetailsService i jedyny
 * UserDetailsPasswordService w kontekście).
 */
@Service
public class JpaUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final AppUserRepository users;

    public JpaUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Nie ma użytkownika " + username));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())           // hash z prefiksem, np. {bcrypt}$2a$10$…
                .roles(user.getRoles().split(","))          // "CUSTOMER,EMPLOYEE" → ROLE_CUSTOMER, ROLE_EMPLOYEE
                .disabled(!user.isEnabled())                // konto wyłączone → DisabledException przy logowaniu
                .build();
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        // newPassword to JUŻ hash (np. {bcrypt}$2a$10$…), nie hasło. Zapisujemy go i oddajemy użytkownika z nowym hashem.
        AppUser entity = users.findByUsername(user.getUsername()).orElseThrow();
        entity.setPasswordHash(newPassword);
        return User.withUserDetails(user).password(newPassword).build();
    }
}
