package s05_validation.val02_valid_in_controllers;

import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Val02ValidInControllers: {@code @WebMvcTest} — wycinek z samymi kontrolerami (bez serwisów i bazy),
 * zapytania wysyła MockMvc. Wybieramy ten test, bo walidacja dzieje się w warstwie web i nie potrzebuje reszty aplikacji.
 */
@WebMvcTest({ProductController.class, PlainController.class, ValidatedClassController.class})
class Val02ValidInControllersTest {

    static final String BAD_BODY = "{\"name\":\"\",\"code\":\"x\",\"priceGrosze\":0}";
    static final String GOOD_BODY = "{\"name\":\"Mleko\",\"code\":\"MLK-0001\",\"priceGrosze\":349}";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("1. @Valid @RequestBody: zły JSON → 400 i MethodArgumentNotValidException; bez @Valid reguły są ignorowane")
    void bodyValidation() throws Exception {
        mockMvc.perform(post("/plain/products").contentType(MediaType.APPLICATION_JSON).content(GOOD_BODY))
                .andExpect(status().isOk());

        var result = mockMvc.perform(post("/plain/products").contentType(MediaType.APPLICATION_JSON).content(BAD_BODY))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);

        // ten sam zły JSON, ale metoda bez @Valid: przechodzi!
        mockMvc.perform(post("/plain/unchecked").contentType(MediaType.APPLICATION_JSON).content(BAD_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. @ExceptionHandler zamienia błędy pól na czytelny JSON (posortowane po nazwie pola)")
    void cleanErrorJson() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(BAD_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Niepoprawne dane"))
                .andExpect(jsonPath("$.problems.length()").value(3))
                .andExpect(jsonPath("$.problems[0].field").value("code"))
                .andExpect(jsonPath("$.problems[0].message").value("Kod ma postać ABC-1234"))
                .andExpect(jsonPath("$.problems[1].field").value("name"))
                .andExpect(jsonPath("$.problems[1].message").value("Nazwa jest wymagana"))
                .andExpect(jsonPath("$.problems[2].field").value("priceGrosze"))
                .andExpect(jsonPath("$.problems[2].message").value("Cena musi być dodatnia"));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(GOOD_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mleko"));
    }

    @Test
    @DisplayName("3. Ograniczenia na @RequestParam/@PathVariable działają BEZ @Validated: HandlerMethodValidationException → 400")
    void parameterValidation() throws Exception {
        var result = mockMvc.perform(get("/plain/items").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(result.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);

        mockMvc.perform(get("/plain/items").param("limit", "5")).andExpect(status().isOk());

        // własna obsługa: nazwy parametrów jako pola
        mockMvc.perform(get("/api/products").param("limit", "500").param("search", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Niepoprawne parametry"))
                .andExpect(jsonPath("$.problems[0].field").value("limit"))
                .andExpect(jsonPath("$.problems[1].field").value("search"));
        mockMvc.perform(get("/api/products/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problems[0].field").value("id"));
        mockMvc.perform(get("/api/products/7")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("3b. @Validated na klasie kontrolera: walidacja idzie przez proxy i rzuca ConstraintViolationException")
    void validatedOnClass() {
        assertThatThrownBy(() -> mockMvc.perform(get("/validated/items").param("limit", "0")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("4. Ciało + ograniczenie parametru w jednej metodzie: wyjątek to HandlerMethodValidationException, błędy ciała mają nazwy pól")
    void bodyAndParameters() throws Exception {
        // id poprawne, ciało złe — mimo to NIE MethodArgumentNotValidException
        var result = mockMvc.perform(put("/api/products/5").contentType(MediaType.APPLICATION_JSON).content(BAD_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Niepoprawne parametry"))
                .andExpect(jsonPath("$.problems[0].field").value("code"))
                .andExpect(jsonPath("$.problems[1].field").value("name"))
                .andExpect(jsonPath("$.problems[2].field").value("priceGrosze"))
                .andReturn();
        assertThat(result.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);

        // id złe i ciało złe — błędy obu rodzajów w jednej odpowiedzi
        mockMvc.perform(put("/api/products/0").contentType(MediaType.APPLICATION_JSON).content(BAD_BODY))
                .andExpect(jsonPath("$.problems.length()").value(4))
                .andExpect(jsonPath("$.problems[3].field").value("priceGrosze"))
                .andExpect(jsonPath("$.problems[2].field").value("name"))
                .andExpect(jsonPath("$.problems[1].field").value("id"));

        mockMvc.perform(put("/api/products/5").contentType(MediaType.APPLICATION_JSON).content(GOOD_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. Przed walidacją: zły typ, zepsuty JSON i brak parametru to osobne wyjątki (też 400)")
    void beforeValidation() throws Exception {
        assertThat(mockMvc.perform(get("/api/products").param("limit", "abc"))
                .andExpect(status().isBadRequest()).andReturn().getResolvedException())
                .isInstanceOf(MethodArgumentTypeMismatchException.class);
        assertThat(mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andReturn().getResolvedException())
                .isInstanceOf(HttpMessageNotReadableException.class);
        assertThat(mockMvc.perform(get("/api/products"))
                .andExpect(status().isBadRequest()).andReturn().getResolvedException())
                .isInstanceOf(MissingServletRequestParameterException.class);
    }
}
