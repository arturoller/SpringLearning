package s06_error_handling.solutions.err03_controller_advice;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import s06_error_handling.err03_controller_advice.PaymentDeclinedException;

import java.util.List;
import java.util.Map;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err03ControllerAdvice — zajrzyj dopiero po własnej próbie! */
public final class Err03ControllerAdviceSolutions {

    private Err03ControllerAdviceSolutions() {
    }

    static List<String> solution1(Map<String, Integer> orderByAdvice) {
        return orderByAdvice.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
    }

    @RestController
    static class Solution2Controller {

        @GetMapping("/api/ex/pay")
        String pay() {
            throw new PaymentDeclinedException("odrzucono");
        }
    }

    @RestControllerAdvice(assignableTypes = Solution2Controller.class)
    static class Solution2Advice {

        @ExceptionHandler(PaymentDeclinedException.class)
        ResponseEntity<Map<String, String>> declined(PaymentDeclinedException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("error", "Płatność odrzucona"));
        }
    }

    @RestController
    static class Solution3Controller {

        @GetMapping("/api/ex/in")
        String in() {
            throw new IllegalStateException("Stan niedozwolony");
        }
    }

    @RestController
    static class Solution3OtherController {

        @GetMapping("/api/ex/out")
        String out() {
            throw new IllegalStateException("Stan niedozwolony");
        }
    }

    @RestControllerAdvice(assignableTypes = Solution3Controller.class)
    @Order(10)
    static class Solution3Advice {

        @ExceptionHandler(IllegalStateException.class)
        ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
