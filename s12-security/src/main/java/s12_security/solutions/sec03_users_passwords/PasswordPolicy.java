package s12_security.solutions.sec03_users_passwords;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rozwiązanie wzorcowe ćwiczeń 1 i 2 lekcji Sec03UsersPasswords. */
public final class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final int BCRYPT_MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static List<String> problems(String username, String password) {
        List<String> problems = new ArrayList<>();
        if (password.length() < MIN_LENGTH) {
            problems.add("ZA_KROTKIE");
        }
        if (password.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
            problems.add("ZAWIERA_LOGIN");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {   // bajty, nie znaki: "ą" = 2 bajty
            problems.add("ZA_DLUGIE");
        }
        return problems;
    }
}
