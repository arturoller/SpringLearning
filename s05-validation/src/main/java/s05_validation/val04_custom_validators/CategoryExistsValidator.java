package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

/**
 * Walidator z wstrzykniętą zależnością. Konstruktor przyjmuje {@link ProductCatalog}. Gdy walidator tworzy Spring
 * ({@code SpringConstraintValidatorFactory}, część {@code LocalValidatorFactoryBean}), zależność zostanie podana. Gdy
 * tworzy go czysta fabryka {@code Validation.buildDefaultValidatorFactory()}, szuka konstruktora bez argumentów i zawodzi.
 * <p>
 * Uwaga: NIE oznaczamy tej klasy {@code @Component} — Spring tworzy walidatory sam, jako osobne obiekty "prototypowe".
 */
public class CategoryExistsValidator implements ConstraintValidator<CategoryExists, String> {

    private final ProductCatalog catalog;

    public CategoryExistsValidator(ProductCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean isValid(String category, ConstraintValidatorContext context) {
        if (category == null) {
            return true;
        }
        if (catalog.categoryExists(category)) {
            return true;
        }
        // Własny parametr komunikatu {categories} — rozszerzenie Hibernate Validatora (unwrap = "zdejmij otoczkę").
        context.unwrap(HibernateConstraintValidatorContext.class)
                .addMessageParameter("categories", String.join(", ", catalog.categories().stream().sorted().toList()));
        return false;
    }
}
