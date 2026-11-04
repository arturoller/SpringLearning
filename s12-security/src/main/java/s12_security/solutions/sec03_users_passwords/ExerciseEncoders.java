package s12_security.solutions.sec03_users_passwords;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Sec03UsersPasswords. */
public final class ExerciseEncoders {

    private ExerciseEncoders() {
    }

    @SuppressWarnings("deprecation")   // NoOpPasswordEncoder jest celowo "przestarzały": tylko do odczytu starych haseł
    public static PasswordEncoder create() {
        Map<String, PasswordEncoder> encoders = Map.of(
                "bcrypt", new BCryptPasswordEncoder(12),
                "noop", NoOpPasswordEncoder.getInstance());
        return new DelegatingPasswordEncoder("bcrypt", encoders);   // "bcrypt" = algorytm dla NOWYCH haseł
    }
}
