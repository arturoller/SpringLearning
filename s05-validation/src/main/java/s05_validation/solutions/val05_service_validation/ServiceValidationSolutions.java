package s05_validation.solutions.val05_service_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import s05_validation.val05_service_validation.ShopPropertiesConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Val05ServiceValidation. */
public final class ServiceValidationSolutions {

    private ServiceValidationSolutions() {
    }

    public static <T> T validateOrThrow(Validator validator, T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return object;
    }

    public static List<String> describe(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " (" + v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName() + ")")
                .sorted()
                .toList();
    }

    public static boolean startsWith(Map<String, String> properties) {
        String[] pairs = properties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ShopPropertiesConfig.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .properties(pairs)
                .run()) {
            return context.isActive();
        } catch (RuntimeException e) {
            return false;   // zła konfiguracja = kontekst się nie utworzył
        }
    }
}
