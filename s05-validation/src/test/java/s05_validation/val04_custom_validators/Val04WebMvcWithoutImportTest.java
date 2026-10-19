package s05_validation.val04_custom_validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Sekcja 4 lekcji Val04CustomValidators — PUŁAPKA: ten sam {@code @WebMvcTest}, ale BEZ {@code @Import(CategoryCatalog.class)}.
 * Wycinek webowy nie skanuje zwykłych beanów, więc walidator nie ma skąd wziąć słownika kategorii.
 */
@WebMvcTest(ShopController.class)
class Val04WebMvcWithoutImportTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("4. W @WebMvcTest bez @Import walidator z zależnością pada: brak beana CategoryCatalog")
    void section4_missingBeanInWebSlice() {
        String body = "{\"name\":\"Kawa\",\"sku\":\"ELE-001\",\"category\":\"DOM\"}";

        assertThatThrownBy(() -> mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body)))
                .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                .rootCause().hasMessageContaining("CategoryCatalog");
    }
}
