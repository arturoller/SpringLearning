package s12_security.sec03_users_passwords;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Błędy kont jako ProblemDetail (s06_error_handling/Err04ProblemDetail). Tylko wyjątki DOMENOWE — bez handlera
 * "łap wszystko", który połknąłby odmowy dostępu (Sec02Authorization, sekcja 3).
 */
@RestControllerAdvice
public class Sec03ErrorHandler {

    @ExceptionHandler(UsernameTakenException.class)
    public ProblemDetail onUsernameTaken(UsernameTakenException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Nazwa zajęta");
        problem.setProperty("code", "USERNAME_TAKEN");
        return problem;
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ProblemDetail onWrongPassword(WrongPasswordException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Niepoprawne hasło");
        problem.setProperty("code", "WRONG_PASSWORD");
        return problem;
    }
}
