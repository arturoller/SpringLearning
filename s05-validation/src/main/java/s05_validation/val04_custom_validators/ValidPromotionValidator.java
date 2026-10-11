package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Walidator klasy Promotion: porównuje pola między sobą. Błędy przypina do KONKRETNYCH pól (validTo, discountGrosze),
 * żeby klient wiedział, które pole poprawić — zamiast jednego błędu „na całym obiekcie”.
 */
public class ValidPromotionValidator implements ConstraintValidator<ValidPromotion, Val04Types.Promotion> {

    @Override
    public boolean isValid(Val04Types.Promotion promotion, ConstraintValidatorContext context) {
        if (promotion == null) {
            return true;   // konwencja: null poprawny
        }
        boolean valid = true;
        // wyłączamy komunikat domyślny (na całym obiekcie), bo dodamy własne, przypięte do pól
        context.disableDefaultConstraintViolation();

        if (promotion.validFrom() != null && promotion.validTo() != null
                && promotion.validFrom().isAfter(promotion.validTo())) {
            context.buildConstraintViolationWithTemplate("Data końca nie może być przed datą początku")
                    .addPropertyNode("validTo")    // property node = węzeł właściwości: ścieżka błędu to "validTo"
                    .addConstraintViolation();     // bez tego wywołania naruszenie NIE powstaje!
            valid = false;
        }
        if (promotion.discountGrosze() > promotion.priceGrosze()) {
            context.buildConstraintViolationWithTemplate("Zniżka nie może być większa od ceny")
                    .addPropertyNode("discountGrosze")
                    .addConstraintViolation();
            valid = false;
        }
        return valid;
    }
}
