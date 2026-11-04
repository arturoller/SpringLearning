package s05_validation.val02_valid_in_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Val02ValidInControllers — warstwa kontrolera. Wycinek: {@code @WebMvcTest} (tylko MVC, bez bazy i bez
 * reszty aplikacji) + {@code @MockitoBean} serwisu: sprawdzamy, czy metoda kontrolera w ogóle została wykonana.
 * Zły JSON, który pokazuje się wszędzie poniżej, to {@code BAD} — trzy błędy naraz (nazwa, cena, ilość).
 */
@WebMvcTest(ProductController.class)
class Val02ValidInControllersTest {

    static final String BAD = "{\"name\":\"\",\"price\":0,\"quantity\":-1}";
    static final String GOOD = "{\"name\":\"Kawa\",\"price\":24.90,\"quantity\":5}";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProductService service;

    // ---------------------------------------------------------------- 1

    @Test
    @DisplayName("Sekcja 1: poprawne dane z @Valid → 201, serwis dostaje DTO")
    void validBodyPasses() throws Exception {
        given(service.create(any())).willReturn(new Product(3, "Kawa", new BigDecimal("24.90"), 5));

        mockMvc.perform(post("/val02/products").contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3));

        verify(service).create(new ProductRequest("Kawa", new BigDecimal("24.90"), 5));
    }

    @Test
    @DisplayName("Sekcja 1: błędne dane z @Valid → 400, a serwis nie zostaje nawet dotknięty")
    void invalidBodyIs400AndMethodDoesNotRun() throws Exception {
        mockMvc.perform(post("/val02/products").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Sekcja 1 (PUŁAPKA): bez @Valid adnotacje są ignorowane — błędne dane dochodzą do serwisu")
    void withoutValidAnnotationsAreIgnored() throws Exception {
        given(service.create(any())).willReturn(new Product(3, "", BigDecimal.ZERO, -1));

        mockMvc.perform(post("/val02/products/unchecked").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isCreated());

        verify(service).create(new ProductRequest("", BigDecimal.ZERO, -1));
    }

    @Test
    @DisplayName("Sekcja 1: puste ciało {} łamie trzy @NotBlank/@NotNull naraz — wszystkie błędy w jednym wyjątku")
    void emptyObjectViolatesAllRequiredFields() throws Exception {
        MvcResult result = mockMvc.perform(post("/val02/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        MethodArgumentNotValidException exception = (MethodArgumentNotValidException) result.getResolvedException();
        assertThat(exception.getBindingResult().getFieldErrors()).extracting(FieldError::getField)
                .containsExactlyInAnyOrder("name", "price", "quantity");
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- 2

    @Test
    @DisplayName("Sekcja 2: MethodArgumentNotValidException zna pole, kod, odrzuconą wartość i komunikat każdego błędu")
    void exceptionContents() throws Exception {
        MvcResult result = mockMvc.perform(post("/val02/products").contentType(MediaType.APPLICATION_JSON).content(BAD)).andReturn();

        MethodArgumentNotValidException exception = (MethodArgumentNotValidException) result.getResolvedException();
        assertThat(exception.getBindingResult().getObjectName()).isEqualTo("productRequest");
        assertThat(exception.getBindingResult().getErrorCount()).isEqualTo(3);

        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .sorted(java.util.Comparator.comparing(FieldError::getField)).toList();
        assertThat(errors).extracting(FieldError::getField).containsExactly("name", "price", "quantity");
        assertThat(errors).extracting(FieldError::getCode).containsExactly("NotBlank", "DecimalMin", "Min");
        assertThat(errors).extracting(FieldError::getRejectedValue).containsExactly("", new BigDecimal("0"), -1);
        assertThat(errors).extracting(FieldError::getDefaultMessage).containsExactly(
                "Nazwa jest wymagana", "Cena musi wynosić co najmniej 0.01", "Ilość nie może być ujemna");
    }

    @Test
    @DisplayName("Sekcja 2: bez własnej obsługi w MockMvc odpowiedź 400 ma puste ciało")
    void defaultBodyIsEmptyInMockMvc() throws Exception {
        mockMvc.perform(post("/val02/products").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }

    // ---------------------------------------------------------------- 3

    @Test
    @DisplayName("Sekcja 3: z BindingResult kontroler sam decyduje — błędy trafiają do jego kodu, nie ma wyjątku")
    void bindingResultKeepsControlInTheController() throws Exception {
        MvcResult result = mockMvc.perform(post("/val02/products/manual").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0]").value("name:NotBlank"))
                .andExpect(jsonPath("$[1]").value("price:DecimalMin"))
                .andExpect(jsonPath("$[2]").value("quantity:Min"))
                .andReturn();

        assertThat(result.getResolvedException()).isNull();   // żaden wyjątek nie poleciał
        verifyNoInteractions(service);                        // ale kontroler świadomie nie zawołał serwisu
    }

    @Test
    @DisplayName("Sekcja 3: z BindingResult poprawne dane przechodzą jak zwykle (201)")
    void bindingResultWithValidData() throws Exception {
        given(service.create(any())).willReturn(new Product(3, "Kawa", new BigDecimal("24.90"), 5));

        mockMvc.perform(post("/val02/products/manual").contentType(MediaType.APPLICATION_JSON).content(GOOD))
                .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- 4

    @Test
    @DisplayName("Sekcja 4: @Min na @PathVariable — id 0 daje 400 i HandlerMethodValidationException, bez @Validated na klasie")
    void pathVariableConstraint() throws Exception {
        MvcResult result = mockMvc.perform(get("/val02/products/0")).andExpect(status().isBadRequest()).andReturn();

        assertThat(result.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);
        HandlerMethodValidationException exception = (HandlerMethodValidationException) result.getResolvedException();
        assertThat(exception.getParameterValidationResults()).singleElement().satisfies(parameterResult -> {
            assertThat(parameterResult.getMethodParameter().getParameterName()).isEqualTo("id");
            assertThat(parameterResult.getArgument()).isEqualTo(0L);
        });
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Sekcja 4: id poprawne → metoda się wykonuje (tu: 404, bo atrapa serwisu nic nie znajduje)")
    void pathVariableValid() throws Exception {
        given(service.find(1)).willReturn(Optional.of(new Product(1, "Kawa", new BigDecimal("24.90"), 5)));

        mockMvc.perform(get("/val02/products/1")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Kawa"));
        mockMvc.perform(get("/val02/products/2")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sekcja 4: @Size na @RequestParam — za długie q daje 400, krótkie (i brak q) przechodzą")
    void requestParamConstraint() throws Exception {
        mockMvc.perform(get("/val02/products").param("q", "abcdefghijk"))   // 11 znaków, limit 10
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class));
        verifyNoInteractions(service);

        mockMvc.perform(get("/val02/products").param("q", "abcdefghij")).andExpect(status().isOk());   // 10 znaków
        mockMvc.perform(get("/val02/products")).andExpect(status().isOk());                            // brak q = null = OK
    }

    @Test
    @DisplayName("Sekcja 4 (PUŁAPKA): gdy metoda ma też ograniczenie na parametrze, błąd CIAŁA to już HandlerMethodValidationException")
    void bodyErrorTurnsIntoMethodValidationException() throws Exception {
        // id poprawne, ciało błędne: bez @Min na id poleciałby MethodArgumentNotValidException (sekcja 1)
        MvcResult bodyOnly = mockMvc.perform(put("/val02/products/1").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(bodyOnly.getResolvedException()).isInstanceOf(HandlerMethodValidationException.class);

        // id błędne i ciało błędne naraz: dwa wyniki walidacji — dla id i dla ciała (ParameterErrors)
        MvcResult both = mockMvc.perform(put("/val02/products/0").contentType(MediaType.APPLICATION_JSON).content(BAD))
                .andExpect(status().isBadRequest()).andReturn();
        HandlerMethodValidationException exception = (HandlerMethodValidationException) both.getResolvedException();
        assertThat(exception.getParameterValidationResults()).hasSize(2);
        assertThat(exception.getParameterValidationResults()).filteredOn(ParameterErrors.class::isInstance)
                .singleElement().satisfies(result -> assertThat(((ParameterErrors) result).getErrorCount()).isEqualTo(3));
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- 5

    @Test
    @DisplayName("Sekcja 5: zepsuty JSON → 400 z HttpMessageNotReadableException, walidacja w ogóle nie rusza")
    void brokenJsonFailsBeforeValidation() throws Exception {
        mockMvc.perform(post("/val02/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(org.springframework.http.converter.HttpMessageNotReadableException.class));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Sekcja 5: wartość złego typu (cena \"abc\") to też błąd JSON (400), a nie błąd walidacji")
    void wrongTypeFailsBeforeValidation() throws Exception {
        mockMvc.perform(post("/val02/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kawa\",\"price\":\"abc\",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(org.springframework.http.converter.HttpMessageNotReadableException.class));
        verifyNoInteractions(service);
    }
}
