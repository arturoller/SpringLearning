package s21_capstone.cap03_library_with_auth.user;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Most między naszą tabelą library_users a Spring Security: {@code UserDetailsService} (usługa danych użytkownika)
 * zwraca login, skrót hasła i role. Hasło porównuje DaoAuthenticationProvider — ten serwis tylko podaje skrót.
 */
@Service
public class LibraryUserDetailsService implements UserDetailsService {

    private final LibraryUserRepository users;

    public LibraryUserDetailsService(LibraryUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        LibraryUser user = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Nie ma użytkownika " + username));
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())   // roles("BIBLIOTEKARZ") → uprawnienie ROLE_BIBLIOTEKARZ
                .build();
    }
}
