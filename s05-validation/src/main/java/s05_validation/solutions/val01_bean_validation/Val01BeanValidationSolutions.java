package s05_validation.solutions.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import s05_validation.val01_bean_validation.ProductForm;

/** Rozwiązania wzorcowe ćwiczeń lekcji Val01BeanValidation — zajrzyj dopiero po własnej próbie! */
public final class Val01BeanValidationSolutions {

    private Val01BeanValidationSolutions() {
    }

    public static List<String> fieldsWithErrors(Validator validator, ProductForm form) {
        return validator.validate(form).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .distinct()
                .sorted()
                .toList();
    }

    public static Map<String, List<String>> messagesByField(Validator validator, Object object) {
        Map<String, List<String>> result = new TreeMap<>();
        for (ConstraintViolation<Object> violation : validator.validate(object)) {
            result.computeIfAbsent(violation.getPropertyPath().toString(), field -> new ArrayList<>())
                    .add(violation.getMessage());
        }
        result.values().forEach(Collections::sort);
        return result;
    }
}
