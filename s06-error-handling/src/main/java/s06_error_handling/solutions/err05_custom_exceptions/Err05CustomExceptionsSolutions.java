package s06_error_handling.solutions.err05_custom_exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import s06_error_handling.err05_custom_exceptions.ErrorCode;
import s06_error_handling.err05_custom_exceptions.OutOfStockException;
import s06_error_handling.err05_custom_exceptions.ShopException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err05CustomExceptions — zajrzyj dopiero po własnej próbie! */
public final class Err05CustomExceptionsSolutions {

    private Err05CustomExceptionsSolutions() {
    }

    record FieldMessage(String field, String message) {
    }

    static HttpStatus solution1(ErrorCode code) {
        return switch (code) {
            case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OUT_OF_STOCK -> HttpStatus.CONFLICT;
            case PAYMENT_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
        };
    }

    static Map<String, List<String>> solution2(List<FieldMessage> violations) {
        Map<String, List<String>> result = new TreeMap<>();
        for (FieldMessage violation : violations) {
            result.computeIfAbsent(violation.field(), field -> new ArrayList<>()).add(violation.message());
        }
        result.values().forEach(messages -> messages.sort(String::compareTo));
        return result;
    }

    static ProblemDetail solution3(ShopException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(solution1(e.getCode()), e.getMessage());
        problem.setProperty("code", e.getCode().name());
        if (e instanceof OutOfStockException stock) {
            problem.setProperty("requested", stock.getRequested());
            problem.setProperty("available", stock.getAvailable());
        }
        return problem;
    }
}
