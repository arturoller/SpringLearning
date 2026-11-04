package s06_error_handling.err04_problem_detail;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Dane nowego klienta (ćwiczenie 5): imię jest wymagane, wiek co najmniej 18. */
public record NewCustomer(
        @NotBlank(message = "Imię jest wymagane") String name,
        @Min(value = 18, message = "Klient musi być pełnoletni") Integer age) {
}
