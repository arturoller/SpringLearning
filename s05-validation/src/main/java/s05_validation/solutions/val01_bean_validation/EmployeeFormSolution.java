package s05_validation.solutions.val01_bean_validation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Rozwiązanie wzorcowe ćwiczenia 2 lekcji Val01BeanValidation (rekord EmployeeForm z adnotacjami). */
public record EmployeeFormSolution(
        @NotBlank String name,
        @NotNull @Positive BigDecimal salary,   // @Positive samo przepuszcza null, więc dodajemy @NotNull
        @NotBlank @Email String email) {        // @NotBlank odrzuca null i pusty tekst, @Email pilnuje formatu
}
