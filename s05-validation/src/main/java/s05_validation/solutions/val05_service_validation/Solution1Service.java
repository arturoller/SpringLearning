package s05_validation.solutions.val05_service_validation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/** Rozwiązanie ćwiczenia 1: @Validated na klasie + ograniczenia przy parametrach. */
@Service
@Validated
public class Solution1Service {

    public int discountedPrice(@Min(0) int priceGrosze, @Min(0) @Max(100) int percent) {
        return priceGrosze - priceGrosze * percent / 100;
    }
}
