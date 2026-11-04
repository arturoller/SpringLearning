package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Walidator klasowy: dostaje CAŁY obiekt {@link Promotion}. Błąd przypina do pola {@code end}, żeby klient zobaczył go
 * przy właściwym polu formularza (a nie "przy całym obiekcie", czyli pod pustą ścieżką).
 */
public class ValidDateRangeValidator implements ConstraintValidator<ValidDateRange, Promotion> {

    @Override
    public boolean isValid(Promotion promotion, ConstraintValidatorContext context) {
        if (promotion == null || promotion.start() == null || promotion.end() == null) {
            return true;   // brak dat = nie nasza sprawa (od tego jest @NotNull na polach)
        }
        if (!promotion.end().isBefore(promotion.start())) {   // ten sam dzień wolno: promocja jednodniowa
            return true;
        }
        context.disableDefaultConstraintViolation();           // wyłącz domyślne naruszenie (na poziomie klasy)…
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("end")                        // …i zgłoś własne, przypięte do pola "end"
                .addConstraintViolation();                     // bez tej linii naruszenie NIE zostanie dodane!
        return false;
    }
}
