package s12_security.solutions.sec03_users_passwords;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import s12_security.sec03_users_passwords.EmployeeAccount;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Rozwiązania wzorcowe ćwiczeń lekcji Sec03UsersPasswords — zajrzyj dopiero po własnej próbie! */
public final class Sec03UsersPasswordsSolutions {

    /** $2a$ / $2b$ / $2y$, dwie cyfry kosztu, $, 53 znaki z alfabetu BCrypta (. / A-Z a-z 0-9). */
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    private Sec03UsersPasswordsSolutions() {
    }

    static boolean solution1(String hash) {
        return hash != null && BCRYPT.matcher(hash).matches();
    }

    static UserDetails solution2(EmployeeAccount account) {
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .roles(account.roles().toArray(String[]::new))
                .disabled(!account.active())
                .build();
    }

    static UserDetailsService solution3(Map<String, EmployeeAccount> accounts) {
        return username -> {   // lambda = implementacja jedynej metody loadUserByUsername
            EmployeeAccount account = accounts.get(username.toLowerCase(Locale.ROOT));
            if (account == null) {
                throw new UsernameNotFoundException("Nie ma użytkownika: " + username);
            }
            return solution2(account);
        };
    }

    static String solution4() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonim";
        }
        return authentication.getName();
    }
}
