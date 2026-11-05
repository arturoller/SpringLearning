package s12_security.sec03_users_passwords;

/** Podane "stare hasło" przy zmianie hasła jest błędne → 400 (Sec03ErrorHandler). Komunikat bez żadnego hasła! */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("Obecne hasło jest niepoprawne");
    }
}
