package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * KnownCategoryValidator = walidator z zależnością. Konstruktor przyjmuje {@link CategoryCatalog}; fabryka walidatorów
 * Springa (SpringConstraintValidatorFactory — zarejestrowana w LocalValidatorFactoryBean) tworzy go jako bean
 * i sama podaje argument. Bez Springa (czysta fabryka Bean Validation) szuka konstruktora bezargumentowego i zawodzi.
 */
public class KnownCategoryValidator implements ConstraintValidator<KnownCategory, String> {

    private final CategoryCatalog catalog;

    public KnownCategoryValidator(CategoryCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || catalog.contains(value);   // null → true: wymagalność to sprawa @NotNull
    }
}
