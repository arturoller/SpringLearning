package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import s05_validation.val04_custom_validators.Val04Types.CatalogProduct;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Val04CustomValidators: {@code @SpringBootTest} — prawdziwy Validator ze Springa tworzy
 * ExistingCategoryValidator z prawdziwym beanem CategoryCatalog.
 */
@SpringBootTest
class Val04CustomValidatorsSpringTest {

    @Autowired
    Validator validator;   // LocalValidatorFactoryBean z autokonfiguracji Boota

    @Test
    @DisplayName("4. Walidator ze Springa wstrzykuje bean CategoryCatalog do ExistingCategoryValidator")
    void springBeanInValidator() {
        assertThat(validator.validate(new CatalogProduct("ABC-1234", "AGD"))).isEmpty();
        assertThat(validator.validate(new CatalogProduct("ABC-1234", null))).isEmpty();   // null poprawny (konwencja)

        Set<ConstraintViolation<CatalogProduct>> violations = validator.validate(new CatalogProduct("ABC-1234", "KOSMOS"));
        assertThat(violations).hasSize(1);
        ConstraintViolation<CatalogProduct> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("category");
        assertThat(violation.getMessage()).isEqualTo("Nie ma takiej kategorii");
    }
}
