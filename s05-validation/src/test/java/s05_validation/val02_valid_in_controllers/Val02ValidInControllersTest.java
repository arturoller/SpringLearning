package s05_validation.val02_valid_in_controllers;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Val02ValidInControllers. {@code @WebMvcTest} — wycinek kontekstu tylko z warstwą webową (kontrolery
 * i MockMvc), bez sieci. Kontrolery nie mają zależności, więc nie trzeba atrap. Treść odpowiedzi 400 na prawdziwym
 * serwerze sprawdza osobna klasa Val02ValidInControllersServerTest.
 */
@WebMvcTest
class Val02ValidInControllersTest {

    /** Zły formularz: pusta nazwa, zły SKU, ujemna cena. */
    static final String BAD_BODY = "{\"name\":\" \",\"sku\":\"x\",\"price\":-1}";
    static final String GOOD_BODY = "{\"name\":\"Kawa ziarnista 1kg\",\"sku\":\"SPO-001\",\"price\":64.99}";

    @Autowired
    MockMvc mockMvc;

    private MvcResult postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    @Test
    @DisplayName("1. @Valid @RequestBody: poprawne dane → 201, złe → 400 i MethodArgumentNotValidException z błędami pól")
    void section1_validBody() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(GOOD_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/SPO-001"));

        MvcResult result = postJson("/api/products", BAD_BODY);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).as("MockMvc nie renderuje strony błędu /error").isEmpty();
        assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);

        MethodArgumentNotValidException ex = (MethodArgumentNotValidException) result.getResolvedException();
        List<String> fields = ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).sorted().toList();
        assertThat(fields).containsExactly("name", "price", "sku");   // wszystkie błędy naraz

        // zły JSON to inny wyjątek — do walidacji w ogóle nie dochodzi
        assertThat(postJson("/api/products", "{nie json").getResolvedException())
                .isInstanceOf(HttpMessageNotReadableException.class);
    }

    @Test
    @DisplayName("2. Bez @Valid ograniczenia w rekordzie są ignorowane: zły formularz przechodzi (200)")
    void section2_missingValid() throws Exception {
        mockMvc.perform(post("/api/products/unchecked").contentType(MediaType.APPLICATION_JSON).content(BAD_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("x"))
                .andExpect(jsonPath("$.name").value(" "));
    }

    @Test
    @DisplayName("3. Ograniczenia na @RequestParam i @PathVariable → HandlerMethodValidationException; zły typ → inny wyjątek")
    void section3_parameterConstraints() throws Exception {
        mockMvc.perform(get("/api/products").param("limit", "5")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.limit").value(10));

        MvcResult zero = mockMvc.perform(get("/api/products").param("limit", "0")).andReturn();
        assertThat(zero.getResponse().getStatus()).isEqualTo(400);
        assertThat(zero.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);

        HandlerMethodValidationException ex = (HandlerMethodValidationException) zero.getResolvedException();
        ParameterValidationResult result = ex.getParameterValidationResults().get(0);
        MethodParameter parameter = result.getMethodParameter();
        assertThat(parameter.getParameterName()).isEqualTo("limit");   // dzięki -parameters z spring-boot-starter-parent
        assertThat(result.getArgument()).isEqualTo(0);

        MvcResult tooBig = mockMvc.perform(get("/api/products").param("limit", "500")).andReturn();
        assertThat(tooBig.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);

        MvcResult badSku = mockMvc.perform(get("/api/products/abc")).andReturn();
        assertThat(badSku.getResponse().getStatus()).isEqualTo(400);
        assertThat(badSku.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);
        mockMvc.perform(get("/api/products/ELE-001")).andExpect(status().isOk());

        // "abc" nie da się zamienić na int: błąd konwersji, nie walidacji
        MvcResult notNumber = mockMvc.perform(get("/api/products").param("limit", "abc")).andReturn();
        assertThat(notNumber.getResponse().getStatus()).isEqualTo(400);
        assertThat(notNumber.getResolvedException()).isInstanceOf(MethodArgumentTypeMismatchException.class);

        // obiekt z parametrów adresu + @Valid → ten sam wyjątek co dla ciała (także gdy typ pola się nie zgadza)
        MvcResult badFilter = mockMvc.perform(get("/api/products/search").param("category", "ab")).andReturn();
        assertThat(badFilter.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);
        MvcResult badFilterType = mockMvc.perform(get("/api/products/search").param("page", "abc")).andReturn();
        assertThat(badFilterType.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);
        mockMvc.perform(get("/api/products/search").param("category", "kawa").param("page", "2"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. @Valid @RequestBody + ograniczenie na innym parametrze → błąd w ciele też daje HandlerMethodValidationException")
    void section4_bodyAndParameter() throws Exception {
        // ciało złe, parametr dobry — pytanie kontrolne 5 dosłownie
        MvcResult badBody = mockMvc.perform(post("/api/products/copies").param("copies", "1")
                .contentType(MediaType.APPLICATION_JSON).content(BAD_BODY)).andReturn();
        assertThat(badBody.getResponse().getStatus()).isEqualTo(400);
        assertThat(badBody.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);

        HandlerMethodValidationException ex = (HandlerMethodValidationException) badBody.getResolvedException();
        assertThat(ex.getBeanResults()).hasSize(1);          // wynik dla ciała — ParameterErrors (błędy pól rekordu)
        assertThat(ex.getValueResults()).isEmpty();          // parametr copies jest poprawny
        ParameterErrors errors = ex.getBeanResults().get(0);
        assertThat(errors.getFieldErrors().stream().map(FieldError::getField).sorted().toList())
                .containsExactly("name", "price", "sku");

        // ciało dobre, parametr zły
        MvcResult badParam = mockMvc.perform(post("/api/products/copies").param("copies", "0")
                .contentType(MediaType.APPLICATION_JSON).content(GOOD_BODY)).andReturn();
        HandlerMethodValidationException exParam = (HandlerMethodValidationException) badParam.getResolvedException();
        assertThat(exParam.getValueResults()).hasSize(1);
        assertThat(exParam.getBeanResults()).isEmpty();

        // wszystko dobrze
        mockMvc.perform(post("/api/products/copies").param("copies", "2")
                        .contentType(MediaType.APPLICATION_JSON).content(GOOD_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("5. @Validated na klasie kontrolera: ograniczenie na parametrze rzuca ConstraintViolationException (nie 400)")
    void section5_validatedOnController() throws Exception {
        // MockMvc nie ma obsługi wyjątku dla ConstraintViolationException, więc wyjątek wychodzi z perform (opakowany)
        assertThatThrownBy(() -> mockMvc.perform(get("/api/validated").param("limit", "0")))
                .hasRootCauseInstanceOf(ConstraintViolationException.class);

        mockMvc.perform(get("/api/validated").param("limit", "1")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("6. @Valid + BindingResult: brak wyjątku, kontroler sam odpowiada 400 z mapą pole → komunikat")
    void section6_bindingResult() throws Exception {
        MvcResult result = postJson("/api/products/manual", BAD_BODY);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException()).as("wyjątku nie ma — błędy trafiły do BindingResult").isNull();
        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("{\"name\":\"Nazwa jest wymagana\",\"price\":\"Cena musi być dodatnia\","
                        + "\"sku\":\"SKU ma format np. ELE-001\"}");

        mockMvc.perform(post("/api/products/manual").contentType(MediaType.APPLICATION_JSON).content(GOOD_BODY))
                .andExpect(status().isOk());
    }
}
