package s05_validation.solutions.val05_service_validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Rozwiązanie ćwiczenia 3: @Validated na klasie, @NotNull @Valid na obowiązkowym polu zagnieżdżonym. */
@ConfigurationProperties("val05.exercise")
@Validated
public record SolutionProperties(
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        @NotNull @Valid Admin admin) {

    public record Admin(@NotBlank @Email String email) {
    }
}
