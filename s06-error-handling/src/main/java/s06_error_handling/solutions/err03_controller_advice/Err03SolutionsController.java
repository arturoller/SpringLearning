package s06_error_handling.solutions.err03_controller_advice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err03ControllerAdvice — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/err03/cw")
public class Err03SolutionsController {

    public static class CustomerNotFoundException extends RuntimeException {

        public CustomerNotFoundException(long id) {
            super("Nie ma klienta " + id);
        }
    }

    public static class OrderLockedException extends RuntimeException {

        public OrderLockedException(long id) {
            super("Zamówienie " + id + " jest zablokowane");
        }
    }

    public static class ExercisePriorityException extends RuntimeException {
    }

    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id) {
        if (id == 1) {
            return "Anna";
        }
        throw new CustomerNotFoundException(id);
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable long id) {
        throw new OrderLockedException(id);
    }

    @GetMapping("/crash")
    public String crash() {
        throw new IllegalStateException("tajny szczegół: hasło do bazy to 'admin'");
    }

    @GetMapping("/priority")
    public String priority() {
        throw new ExercisePriorityException();
    }
}
