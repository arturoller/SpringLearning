package s05_validation.val04_custom_validators;

import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Dowód, że walidator używa DOKŁADNIE tego beana, który jest w kontekście — podmieniamy go atrapą (mockiem). */
@SpringBootTest(classes = Val04Application.class)
class Val04MockCatalogTest {

    @MockitoBean
    ProductCatalog catalog;   // @MockitoBean = atrapa wstawiona do kontekstu zamiast prawdziwego beana

    @Autowired
    Validator validator;

    @Test
    @DisplayName("Sekcja 4: walidator pyta atrapę katalogu, więc dowolna kategoria może być uznana za istniejącą")
    void validatorUsesTheBeanFromTheContext() {
        when(catalog.categoryExists("zupa")).thenReturn(true);

        assertThat(validator.validate(new NewProduct(null, "zupa"))).isEmpty();
        verify(catalog).categoryExists("zupa");

        when(catalog.categories()).thenReturn(Set.of("a"));
        assertThat(validator.validate(new NewProduct(null, "inna"))).hasSize(1);
    }
}
