package s05_validation.val01_bean_validation;

import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 7 lekcji Val01BeanValidation: {@code @SpringBootTest} (cały kontekst) — Boot sam tworzy bean Validator.
 * Konfigurację znajduje w Val01Application (ten sam pakiet).
 */
@SpringBootTest
class Val01BeanValidationSpringTest {

    @Autowired
    Validator validator;   // jakarta.validation.Validator — wstrzyknięty przez Springa

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("7. Spring Boot udostępnia gotowy bean Validator, którego można użyć jak w zwykłym teście")
    void validatorIsABean() {
        assertThat(context.getBeanNamesForType(Validator.class)).contains("defaultValidator");
        assertThat(Val01BeanValidation.invalidFields(validator, Val01BeanValidation.NewProduct.valid())).isEmpty();
        assertThat(Val01BeanValidation.invalidFields(validator,
                new Val01BeanValidation.NewProduct("ab", "zly", 349, 10, null, null, null)))
                .containsExactly("code", "name");
    }
}
