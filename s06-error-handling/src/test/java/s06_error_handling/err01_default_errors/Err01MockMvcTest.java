package s06_error_handling.err01_default_errors;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Sekcja 4 lekcji Err01DefaultErrors: co widać w MockMvc. Najwęższy wycinek ({@code @WebMvcTest}), bo sprawdzamy tylko
 * warstwę web — i właśnie ona NIE przekierowuje błędów do {@code /error}.
 */
@WebMvcTest(ShopErrorsController.class)
class Err01MockMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Sekcja 4: wyjątek bez obsługi nie daje w MockMvc statusu 500, tylko wyjątek wyrzucony do testu")
    void unhandledExceptionReachesTheTest() {
        assertThatThrownBy(() -> mockMvc.perform(get("/err01/boom")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Błąd wewnętrzny: połączenie z bazą db-prod-01 odrzucone");
    }

    @Test
    @DisplayName("Sekcja 4: ResponseStatusException i @ResponseStatus dają w MockMvc właściwy status, ale PUSTE ciało")
    void statusIsRightBodyIsEmpty() throws Exception {
        MockHttpServletResponse notFound = mockMvc.perform(get("/err01/products/9")).andReturn().getResponse();
        MockHttpServletResponse conflict = mockMvc.perform(get("/err01/stock/kawa")).andReturn().getResponse();

        assertThat(notFound.getStatus()).isEqualTo(404);
        assertThat(notFound.getContentAsString()).isEmpty();
        assertThat(notFound.getErrorMessage()).isEqualTo("Nie ma produktu o id 9");   // opis jest, ale tylko w obiekcie odpowiedzi
        assertThat(conflict.getStatus()).isEqualTo(409);
        assertThat(conflict.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("Sekcja 4: nieznana ścieżka w MockMvc → 404 i puste ciało (bez JSON-a z /error)")
    void unknownPathHasNoBody() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/err01/nie-ma-takiej-sciezki")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).isEmpty();
    }
}
