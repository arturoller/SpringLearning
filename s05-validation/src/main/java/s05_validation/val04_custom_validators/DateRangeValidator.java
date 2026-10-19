package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * DateRangeValidator = logika {@link ValidDateRange}. Sprawdza cały obiekt (typ {@link HasDateRange}). Gdy
 * {@code reportOnField = true}, błąd „przypina” do pola {@code to} — klient widzi go przy konkretnym polu.
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, HasDateRange> {

    private boolean reportOnField;

    /** initialize = inicjalizacja: Bean Validation wywołuje ją raz, z adnotacją, na której stoi walidator. */
    @Override
    public void initialize(ValidDateRange annotation) {
        this.reportOnField = annotation.reportOnField();
    }

    @Override
    public boolean isValid(HasDateRange value, ConstraintValidatorContext context) {
        if (value == null || value.from() == null || value.to() == null) {
            return true;   // brak dat to sprawa @NotNull na polach, nie tej reguły
        }
        if (!value.from().isAfter(value.to())) {
            return true;   // od <= do: poprawnie (ten sam dzień jest dozwolony)
        }
        if (reportOnField) {
            context.disableDefaultConstraintViolation();   // wyłącz domyślne naruszenie (na poziomie klasy)...
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("to")                  // ...i zgłoś to samo naruszenie na polu "to"
                    .addConstraintViolation();
        }
        return false;
    }
}
