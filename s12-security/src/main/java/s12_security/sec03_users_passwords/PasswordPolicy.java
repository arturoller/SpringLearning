package s12_security.sec03_users_passwords;

import java.util.List;

/**
 * SZKIELET ĆWICZEŃ 1 i 2 lekcji Sec03UsersPasswords: prosta polityka haseł sprawdzana przy rejestracji.
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    /**
     * Lista problemów z hasłem (pusta = hasło w porządku), w tej kolejności:
     * <ul>
     *   <li>ćwiczenie 1: {@code "ZA_KROTKIE"} — mniej niż 12 znaków;</li>
     *   <li>ćwiczenie 1: {@code "ZAWIERA_LOGIN"} — hasło zawiera nazwę użytkownika (bez względu na wielkość liter);</li>
     *   <li>ćwiczenie 2: {@code "ZA_DLUGIE"} — więcej niż 72 BAJTY w UTF-8 (limit BCrypt).</li>
     * </ul>
     */
    public static List<String> problems(String username, String password) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
