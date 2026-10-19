package s05_validation.solutions.val05_service_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import java.util.Map;
import java.util.TreeMap;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Val05ServiceValidation — zajrzyj dopiero po własnej próbie! */
public final class Val05ServiceValidationSolutions {

    private Val05ServiceValidationSolutions() {
    }

    public static Map<String, String> violationsByField(ConstraintViolationException exception) {
        Map<String, String> result = new TreeMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String last = null;
            for (Path.Node node : violation.getPropertyPath()) {
                last = node.getName();   // ostatni element ścieżki, np. "name" z add.command.name
            }
            result.merge(last, violation.getMessage(), (a, b) -> a.compareTo(b) <= 0 ? a : b);
        }
        return result;
    }
}
