package s05_validation.solutions.val02_valid_in_controllers;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.TreeMap;

/** Rozwiązanie wzorcowe ćwiczenia 3 lekcji Val02ValidInControllers — zajrzyj dopiero po własnej próbie! */
public final class Val02ValidInControllersSolutions {

    private Val02ValidInControllersSolutions() {
    }

    public static Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
        Map<String, String> result = new TreeMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            result.merge(error.getField(), String.valueOf(error.getDefaultMessage()),
                    (a, b) -> a.compareTo(b) <= 0 ? a : b);   // przy kilku błędach pola — najmniejszy leksykograficznie
        }
        return result;
    }
}
