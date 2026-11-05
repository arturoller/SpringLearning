package s05_validation.val02_valid_in_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sekcja 2 lekcji Val02ValidInControllers: prosty @ExceptionHandler w kontrolerze (zapowiedź s06). */
@WebMvcTest(FriendlyProductController.class)
class Val02FriendlyTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;

    @Test
    @DisplayName("Sekcja 2: błędne ciało → 400 i mapa pole → komunikat, posortowana po nazwie pola")
    void bodyErrorsAsSortedMap() throws Exception {
        MvcResult result = mockMvc.perform(post("/val02/friendly/products").contentType(MediaType.APPLICATION_JSON)
                        .content(Val02ValidInControllersTest.BAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Nazwa jest wymagana"))
                .andExpect(jsonPath("$.price").value("Cena musi wynosić co najmniej 0.01"))
                .andExpect(jsonPath("$.quantity").value("Ilość nie może być ujemna"))
                .andReturn();

        // kolejność kluczy w JSON: name, price, quantity — alfabetycznie, więc za każdym razem taka sama
        String json = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json.indexOf("\"name\"")).isLessThan(json.indexOf("\"price\""));
        assertThat(json.indexOf("\"price\"")).isLessThan(json.indexOf("\"quantity\""));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Sekcja 4: błędny parametr (@Min na id) → 400 i lista komunikatów z HandlerMethodValidationException")
    void parameterErrorsAsList() throws Exception {
        mockMvc.perform(get("/val02/friendly/products/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("Id musi być dodatnie"));
        verifyNoInteractions(service);
    }
}
