package s05_validation.val04_custom_validators;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Val04CustomValidators. {@code @SpringBootTest}, bo sprawdzamy współpracę walidatora z kontenerem Springa
 * (wstrzykiwanie zależności do walidatora) — do samych adnotacji kontekst nie jest potrzebny.
 */
@SpringBootTest(classes = Val04Application.class)
class Val04SpringTest {

    @Autowired
    Validator validator;   // defaultValidator = LocalValidatorFactoryBean, który tworzy walidatory przez Springa

    @Test
    @DisplayName("Sekcja 4: walidator tworzony przez Springa dostaje wstrzyknięty ProductCatalog")
    void validatorGetsDependency() {
        assertThat(validator.validate(new NewProduct("ABC-1234", "kawa"))).isEmpty();

        Set<ConstraintViolation<NewProduct>> violations = validator.validate(new NewProduct("ABC-1234", "zupa"));

        assertThat(violations).hasSize(1);
        ConstraintViolation<NewProduct> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("category");
        assertThat(violation.getMessage()).isEqualTo("Nieznana kategoria \"zupa\"; dostępne: akcesoria, herbata, kawa");
    }

    @Test
    @DisplayName("Sekcja 4: ograniczenie bez zależności działa tak samo w kontekście Springa; null przechodzi")
    void sameConstraintsWork() {
        assertThat(validator.validate(new NewProduct("zły", null))).hasSize(1);
        assertThat(validator.validate(new NewProduct(null, null))).isEmpty();
    }
}
