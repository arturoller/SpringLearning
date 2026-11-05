package s06_error_handling.err04_problem_detail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 3 lekcji Err04ProblemDetail: wbudowana obsługa Boota po włączeniu {@code spring.mvc.problemdetails.enabled=true}.
 * Ustawiamy ją tylko w tym teście ({@code properties = …}), a kontekst budujemy ręcznie ({@code @ContextConfiguration}):
 * sam kontroler, bez {@code Err04Advice} — inaczej nasz advice wyłączyłby wbudowany mechanizm (Err04OwnAdviceDisablesBuiltInTest).
 */
@WebMvcTest(properties = "spring.mvc.problemdetails.enabled=true")
@ContextConfiguration(classes = ShopController.class)
class Err04BuiltInHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 3: właściwość tworzy bean problemDetailsExceptionHandler")
    void builtInHandlerExists() {
        assertThat(context.containsBean("problemDetailsExceptionHandler")).isTrue();
    }

    @Test
    @DisplayName("Sekcja 3: 405, 415, 400 (zły typ, zepsuty JSON) i ErrorResponse jako application/problem+json")
    void springExceptionsAsProblemDetail() throws Exception {
        mockMvc.perform(delete("/err04/products/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"))
                .andExpect(jsonPath("$.detail").value("Method 'DELETE' is not supported."))
                .andExpect(jsonPath("$.instance").value("/err04/products/1"));
        mockMvc.perform(post("/err04/products").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/err04/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/err04/nie-ma-takiej-sciezki"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/err04/products/1/reserve"))
                .andExpect(status().isConflict())                                        // ResponseStatusException
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Produkt 1 jest już zarezerwowany"));
        mockMvc.perform(get("/err04/products/1/archive"))
                .andExpect(status().isGone())                                            // ErrorResponseException
                .andExpect(jsonPath("$.code").value("PRODUCT_ARCHIVED"));
    }

    @Test
    @DisplayName("Sekcja 3: wbudowany handler zna błąd walidacji, ale nie poda listy pól — dlatego piszemy własny (sekcja 4)")
    void validationWithoutFieldList() throws Exception {
        mockMvc.perform(post("/err04/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid request content."))
                .andExpect(jsonPath("$.*", hasSize(4)));                                 // detail, instance, status, title — nic o polach
    }

    @Test
    @DisplayName("Sekcja 3: wyjątki WŁASNE (domenowe) wbudowany handler zostawia bez obsługi")
    void domainExceptionsAreNotHandled() {
        assertThatThrownBy(() -> mockMvc.perform(get("/err04/products/9")))
                .hasCauseInstanceOf(ProductNotFoundException.class);                     // MockMvc: ServletException; serwer: 500 z /error
    }
}
