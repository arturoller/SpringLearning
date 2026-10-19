package s06_error_handling.solutions.err03_controller_advice;

import s06_error_handling.err03_controller_advice.Err03ControllerAdvice.AdviceDef;
import s06_error_handling.err03_controller_advice.Err03ControllerAdvice.FieldProblem;
import s06_error_handling.err03_controller_advice.ProductNotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Rozwiązania wzorcowe ćwiczeń lekcji Err03ControllerAdvice — zajrzyj dopiero po własnej próbie! */
public final class Err03ControllerAdviceSolutions {

    private Err03ControllerAdviceSolutions() {
    }

    static int solution1(Throwable e) {
        return switch (e) {
            case ProductNotFoundException p -> 404;
            case IllegalArgumentException i -> 400;
            case UnsupportedOperationException u -> 501;
            default -> 500;
        };
    }

    static Map<String, String> solution2(List<FieldProblem> problems) {
        Map<String, String> result = new TreeMap<>();
        problems.stream()
                .sorted(Comparator.comparing(FieldProblem::message))   // alfabetycznie po komunikacie, żeby łączenie było powtarzalne
                .forEach(p -> result.merge(p.field(), p.message(), (a, b) -> a + "; " + b));
        return result;
    }

    static String solution3(List<AdviceDef> advices, Class<?> controller, Class<? extends Throwable> thrown) {
        return advices.stream()
                .sorted(Comparator.comparingInt(AdviceDef::order))   // sorted() pracuje na kopii — lista wejściowa się nie zmienia
                .filter(a -> a.controllers().isEmpty() || a.controllers().contains(controller))
                .filter(a -> a.handles().stream().anyMatch(h -> h.isAssignableFrom(thrown)))
                .map(AdviceDef::name)
                .findFirst()
                .orElse(null);
    }
}
