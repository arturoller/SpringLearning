package s05_validation.val04_custom_validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Własne ograniczenia w kontrolerze: {@code @WebMvcTest}. Katalog kategorii (@Component) nie należy do wycinka web,
 * więc podajemy atrapę {@code @MockitoBean} — walidator ExistingCategoryValidator dostanie ją przez Springa.
 */
@WebMvcTest(CatalogController.class)
class Val04CustomValidatorsWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CategoryCatalog catalog;

    private List<String> errorFields(String url, String json, int expectedStatus) throws Exception {
        var result = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus)).andReturn();
        if (result.getResolvedException() instanceof MethodArgumentNotValidException e) {
            return e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()).sorted().toList();
        }
        return List.of();
    }

    @Test
    @DisplayName("4. Własne ograniczenia działają w kontrolerze: zły kod i nieznana kategoria → 400; reguła klasowa → błąd na polu")
    void customConstraintsInController() throws Exception {
        given(catalog.exists("AGD")).willReturn(true);   // atrapa: tylko AGD istnieje

        errorFields("/api/catalog-products", "{\"code\":\"ABC-1234\",\"category\":\"AGD\"}", 200);
        assertThat(errorFields("/api/catalog-products", "{\"code\":\"abc\",\"category\":\"KOSMOS\"}", 400))
                .containsExactly("category", "code");

        String badRange = "{\"validFrom\":\"2026-06-01\",\"validTo\":\"2026-05-01\",\"priceGrosze\":1000,\"discountGrosze\":300}";
        assertThat(errorFields("/api/promotions", badRange, 400)).containsExactly("validTo");
    }
}
