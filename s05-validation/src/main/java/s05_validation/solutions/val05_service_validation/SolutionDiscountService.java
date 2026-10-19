package s05_validation.solutions.val05_service_validation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Val05ServiceValidation. */
@Service
@Validated   // bez tego ograniczenia poniżej byłyby tylko ozdobą: Spring nie owinąłby beana w proxy walidacji
public class SolutionDiscountService {

    public BigDecimal apply(@NotNull @Positive BigDecimal price, @Min(0) @Max(100) int percent) {
        return price.multiply(BigDecimal.valueOf(100L - percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
