package s12_security.sec03_users_passwords;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SZKIELET ĆWICZENIA 3 lekcji Sec03UsersPasswords: własny {@code DelegatingPasswordEncoder}.
 */
public final class ExerciseEncoders {

    private ExerciseEncoders() {
    }

    /**
     * Ćwiczenie 3: koder, który NOWE hasła koduje BCryptem o koszcie 12 z prefiksem {@code {bcrypt}}, a przy sprawdzaniu
     * rozumie też stare hasła {@code {noop}…}. Hash {@code {noop}} ma być oznaczony do przepisania ({@code upgradeEncoding}).
     */
    public static PasswordEncoder create() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
