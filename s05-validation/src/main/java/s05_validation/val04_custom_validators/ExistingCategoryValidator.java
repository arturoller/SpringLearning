package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Walidator z zależnością: dostaje {@link CategoryCatalog} przez konstruktor. Tworzy go SpringConstraintValidatorFactory
 * (część LocalValidatorFactoryBean), która potrafi wstrzyknąć beany. Zwykła fabryka Hibernate Validatora wymaga
 * konstruktora bez parametrów i tu by zawiodła.
 */
public class ExistingCategoryValidator implements ConstraintValidator<ExistingCategory, String> {

    private final CategoryCatalog catalog;

    public ExistingCategoryValidator(CategoryCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || catalog.exists(value);   // null poprawny — konwencja
    }
}
