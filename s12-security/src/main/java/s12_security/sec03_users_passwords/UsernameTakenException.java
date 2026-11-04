package s12_security.sec03_users_passwords;

/** Nazwa użytkownika jest już zajęta → 409 Conflict (Sec03ErrorHandler). */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Nazwa użytkownika " + username + " jest już zajęta");
    }
}
