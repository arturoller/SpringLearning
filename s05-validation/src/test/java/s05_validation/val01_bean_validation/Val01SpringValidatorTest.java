package s05_validation.val01_bean_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Val01BeanValidation. {@code @SpringBootTest}, bo sprawdzamy właśnie autokonfigurację Boota
 * (ValidationAutoConfiguration) — do samych adnotacji kontekst nie jest potrzebny (zob. Val01BeanValidationTest).
 */
@SpringBootTest(classes = Val01Application.class)
class Val01SpringValidatorTest {

    @Autowired
    Validator validator;   // jakarta.validation.Validator — wstrzyknięty przez Springa

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 4: Boot tworzy bean defaultValidator (LocalValidatorFactoryBean) i wstrzykuje go jako Validator")
    void bootCreatesValidatorBean() {
        assertThat(validator).isInstanceOf(LocalValidatorFactoryBean.class);
        assertThat(context.getBeanNamesForType(Validator.class)).containsExactly("defaultValidator");
        // ten sam obiekt jest też "walidatorem Springa" (org.springframework.validation.Validator)
        assertThat(context.getBean("defaultValidator")).isSameAs(validator).isInstanceOf(org.springframework.validation.Validator.class);
    }

    @Test
    @DisplayName("Sekcja 4: wstrzyknięty Validator waliduje dokładnie tak samo jak ręcznie zbudowany")
    void injectedValidatorValidates() {
        Set<ConstraintViolation<ProductForm>> violations =
                validator.validate(new ProductForm("", new BigDecimal("0.00"), 0, null));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "name", "price", "quantity");
        assertThat(validator.validate(new ProductForm("Kawa", new BigDecimal("24.90"), 5, null))).isEmpty();
    }
}
