package s05_validation.val05_service_validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Val05ServiceValidation. {@code @SpringBootTest} — walidacja metod działa przez proxy, więc potrzebny jest
 * prawdziwy kontekst Springa z serwisem (zwykły {@code new ProductService()} NIE miałby walidacji). MockMvc dostajemy
 * przez {@code @AutoConfigureMockMvc}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Val05ServiceValidationTest {

    @Autowired
    ProductService service;

    @Autowired
    UnvalidatedService unvalidated;

    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvc mockMvc;

    private static Set<String> paths(ConstraintViolationException exception) {
        Set<String> result = new TreeSet<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            result.add(violation.getPropertyPath().toString());
        }
        return result;
    }

    @Test
    @DisplayName("1. @Validated na klasie → bean jest proxy (CGLIB) tworzonym przez MethodValidationPostProcessor z Boota")
    void section1_proxy() {
        assertThat(context.getBeanNamesForType(MethodValidationPostProcessor.class))
                .containsExactly("methodValidationPostProcessor");
        assertThat(AopUtils.isAopProxy(service)).isTrue();
        assertThat(AopUtils.isCglibProxy(service)).isTrue();
        assertThat(service.getClass()).isNotEqualTo(ProductService.class).hasSuperclass(ProductService.class);
    }

    @Test
    @DisplayName("2. Zły parametr → ConstraintViolationException ze ścieżkami metoda.parametr.pole; ciało metody się nie wykonuje")
    void section2_parameterViolations() {
        int before = service.addedCount();

        assertThatThrownBy(() -> service.add(new NewProduct(" ", "x", BigDecimal.ZERO)))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(paths((ConstraintViolationException) e))
                        .containsExactly("add.command.name", "add.command.price", "add.command.sku"));
        assertThatThrownBy(() -> service.add(null))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(paths((ConstraintViolationException) e)).containsExactly("add.command"));

        assertThat(service.addedCount()).as("metoda add nie wykonała się ani razu").isEqualTo(before);

        Product added = service.add(new NewProduct("Kawa ziarnista 1kg", "SPO-001", new BigDecimal("64.99")));
        assertThat(added.sku()).isEqualTo("SPO-001");
        assertThat(service.addedCount()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("3. Parametr prosty i ograniczenie WYNIKU: ścieżki stockOf.sku i stockOf.<return value>")
    void section3_returnValue() {
        assertThat(service.stockOf("ELE-001")).isEqualTo(7);

        assertThatThrownBy(() -> service.stockOf("zle"))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(paths((ConstraintViolationException) e)).containsExactly("stockOf.sku"));

        // „uszkodzone dane” w serwisie (ujemny stan) → wynik łamie @PositiveOrZero, wyjątek nie dopuszcza złej wartości dalej
        assertThatThrownBy(() -> service.stockOf("BAD-001"))
                .isInstanceOf(ConstraintViolationException.class)
                .satisfies(e -> assertThat(paths((ConstraintViolationException) e)).containsExactly("stockOf.<return value>"));
    }

    @Test
    @DisplayName("4. PUŁAPKA: wywołanie metody z tej samej klasy (this.) omija proxy — ograniczenie nie działa")
    void section4_selfInvocation() {
        // z zewnątrz (przez proxy) ograniczenie działa
        assertThatThrownBy(() -> service.normalize(" ")).isInstanceOf(ConstraintViolationException.class);

        // ta sama metoda wywołana przez describe() → this.normalize(" ") — BEZ walidacji (a nawet bez błędu)
        assertThat(service.describe(" ")).isEqualTo("SKU: ");
        // pytanie kontrolne 4: dla null dopiero w środku normalize poleci NullPointerException, nie błąd walidacji
        assertThatThrownBy(() -> service.describe(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("5. PUŁAPKA: bez @Validated na klasie ograniczenia na parametrach są ignorowane, a bean nie jest proxy")
    void section5_missingValidated() {
        assertThat(AopUtils.isAopProxy(unvalidated)).isFalse();
        assertThat(unvalidated.shout("")).isEqualTo("!");   // @NotBlank na parametrze nikogo nie obchodzi
    }

    @Test
    @DisplayName("6. Kontroler waliduje kształt zapytania (400), a naruszenie z serwisu bez obsługi wychodzi jako wyjątek (500)")
    void section6_controllerVersusService() throws Exception {
        int before = service.addedCount();

        // zły formularz zatrzymał kontroler (@Valid) — serwis w ogóle nie został wywołany
        MvcResult bad = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"sku\":\"x\",\"price\":-1}")).andReturn();
        assertThat(bad.getResponse().getStatus()).isEqualTo(400);
        assertThat(service.addedCount()).isEqualTo(before);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Czekolada gorzka\",\"sku\":\"SPO-002\",\"price\":7.49}"))
                .andExpect(status().isCreated());
        assertThat(service.addedCount()).isEqualTo(before + 1);

        // kontroler nie sprawdza SKU z adresu; sprawdził dopiero serwis → ConstraintViolationException (bez obsługi: 500)
        mockMvc.perform(get("/api/products/ELE-001/stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7));
        assertThatThrownBy(() -> mockMvc.perform(get("/api/products/zle/stock")))
                .hasRootCauseInstanceOf(ConstraintViolationException.class);
    }
}
