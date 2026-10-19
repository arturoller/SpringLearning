package s05_validation.val05_service_validation;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DiscountService = SZKIELET do ćwiczenia 1 lekcji Val05ServiceValidation (rabat procentowy).
 * TODO: spraw, żeby serwis rzucał ConstraintViolationException dla ceny niedodatniej (albo null) i dla procentu poza
 * zakresem 0–100. Rozwiązanie wzorcowe: solutions.val05_service_validation.
 */
@Service   // TODO ćwiczenie 1: czego brakuje na klasie, żeby ograniczenia na parametrach w ogóle działały?
public class DiscountService {

    // TODO ćwiczenie 1: dopisz ograniczenia do parametrów
    public BigDecimal apply(BigDecimal price, int percent) {
        return price.multiply(BigDecimal.valueOf(100L - percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
