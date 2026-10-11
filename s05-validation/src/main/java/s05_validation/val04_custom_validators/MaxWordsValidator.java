package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** SZKIELET ćwiczenia 2 z lekcji Val04CustomValidators (opis: Val04CustomValidators). */
public class MaxWordsValidator implements ConstraintValidator<MaxWords, String> {

    @Override
    public void initialize(MaxWords annotation) {
        // TODO: odczytaj z adnotacji dozwoloną liczbę słów i zapamiętaj ją w polu klasy
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
