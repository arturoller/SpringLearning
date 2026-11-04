package s12_security.sec03_users_passwords;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Konta lekcji Sec03UsersPasswords: rejestracja (publiczna), "kim jestem" i zmiana hasła (tylko zalogowani).
 * Kontroler zwraca wyłącznie {@link UserView} — nigdy encji {@link AppUser}.
 */
@RestController
@RequestMapping("/sec03")
public class UserAccountController {

    private final UserAccountService accounts;

    public UserAccountController(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegistrationRequest request) {
        return accounts.register(request);
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal UserDetails user) {
        return new UserView(user.getUsername(), user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))     // bez FACTOR_PASSWORD (Sec01Basics, sekcja 5)
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserDetails user, @Valid @RequestBody PasswordChangeRequest request) {
        accounts.changePassword(user.getUsername(), request);
    }
}
