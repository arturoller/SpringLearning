package s05_validation.solutions.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import s05_validation.val01_bean_validation.ProductForm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Val01BeanValidation — zajrzyj dopiero po własnej próbie! */
public final class ValidationExercisesSolutions {

    private ValidationExercisesSolutions() {
    }

    public static boolean isValid(Validator validator, Object object) {
        return validator.validate(object).isEmpty();
    }

    public static List<String> invalidFields(Validator validator, Object object) {
        return validator.validate(object).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .distinct()
                .sorted()
                .toList();
    }

    public static Map<String, List<String>> constraintsByField(Validator validator, Object object) {
        Map<String, List<String>> result = new TreeMap<>();
        for (ConstraintViolation<Object> violation : validator.validate(object)) {
            result.computeIfAbsent(violation.getPropertyPath().toString(), key -> new java.util.ArrayList<>())
                    .add(constraintName(violation));
        }
        result.values().forEach(java.util.Collections::sort);
        return result;
    }

    public static boolean isPriceAllowed(Validator validator, BigDecimal price) {
        return validator.validateValue(ProductForm.class, "price", price).isEmpty();
    }

    public static List<String> report(Validator validator, Object object) {
        Set<ConstraintViolation<Object>> violations = validator.validate(object);
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + constraintName(v) + " (" + v.getInvalidValue() + ")")
                .sorted()
                .collect(Collectors.toList());
    }

    private static String constraintName(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    }
}
