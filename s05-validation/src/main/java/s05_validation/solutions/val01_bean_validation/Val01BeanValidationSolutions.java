package s05_validation.solutions.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Val01BeanValidation (test: Val01BeanValidationSolutionsTest). */
public final class Val01BeanValidationSolutions {

    private Val01BeanValidationSolutions() {
    }

    /** Rozwiązanie 1: @NotBlank na nazwie, @NotBlank + @Email na e-mailu (sam @Email przepuszczałby null), @Min(18) na wieku. */
    public record Solution1Customer(
            @NotBlank String name,
            @NotBlank @Email String email,
            @Min(18) int age) {
    }

    /** Rozwiązanie 2: pole i komunikat, posortowane tekstowo. */
    public static List<String> solution2(Validator validator, Object bean) {
        return validator.validate(bean).stream()
                .map((ConstraintViolation<Object> v) -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
    }

    /**
     * Rozwiązanie 3: dwie adnotacje na loginie to dwa niezależne naruszenia; @NotNull łapie null (reszta go przepuszcza).
     * Pensja: @Min i @Max z tym samym komunikatem; data: @Past (null przechodzi — pole opcjonalne w tym zadaniu).
     */
    public record Solution3Employee(
            @NotNull @Size(min = 3, max = 12, message = "Login: 3-12 znaków")
            @Pattern(regexp = "[a-z]+", message = "Login: tylko małe litery")
            String login,
            @Min(value = 1, message = "Pensja: od 1 do 1000000") @Max(value = 1_000_000, message = "Pensja: od 1 do 1000000")
            int salary,
            @Past(message = "Data zatrudnienia musi być z przeszłości") LocalDate hiredOn) {
    }
}
