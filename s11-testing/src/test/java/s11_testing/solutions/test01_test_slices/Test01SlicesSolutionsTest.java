package s11_testing.solutions.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import s11_testing.test01_test_slices.Product;
import s11_testing.test01_test_slices.ProductController;
import s11_testing.test01_test_slices.ProductErrorAdvice;
import s11_testing.test01_test_slices.ProductRepository;
import s11_testing.test01_test_slices.ProductRequest;
import s11_testing.test01_test_slices.ProductService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rozwiązania wzorcowe ćwiczeń Test01TestSlices. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
class Test01SlicesSolutionsTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    private static Product product(long id, String name, String price) {
        Product product = new Product(name, new BigDecimal(price), NOW);
        ReflectionTestUtils.setField(product, "id", id);   // id normalnie nadaje baza
        return product;
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Exercise1 {

        @Mock
        ProductRepository repository;

        @Test
        @DisplayName("Wzorzec 1: rabat 0% nie zmienia ceny, rabat 100% daje 0,00")
        void discountBounds() {
            given(repository.findById(1L)).willReturn(Optional.of(product(1, "Kawa", "24.90")));
            ProductService service = new ProductService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

            assertThat(service.priceAfterDiscount(1L, 0)).isEqualByComparingTo("24.90");
            assertThat(service.priceAfterDiscount(1L, 100)).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @WebMvcTest
    @Import({ProductController.class, ProductErrorAdvice.class})
    class Exercises2And3 {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        ProductService service;

        @Test
        @DisplayName("Wzorzec 2: GET lista → JSON z dwoma produktami z atrapy")
        void list() throws Exception {
            given(service.findAll()).willReturn(List.of(product(1, "Kawa", "24.90"), product(2, "Herbata", "15.99")));

            mockMvc.perform(get("/test01/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[1].name").value("Herbata"));
        }

        @Test
        @DisplayName("Wzorzec 3: POST z pustą nazwą → 400 i serwis nie jest wołany")
        void blankName() throws Exception {
            mockMvc.perform(post("/test01/products").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\" \",\"price\":1}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }
    }

    @Nested
    @JsonTest
    class Exercise4 {

        @Autowired
        JacksonTester<ProductRequest> json;

        @Test
        @DisplayName("Wzorzec 4: nieznane pole jest ignorowane, brak ceny daje null")
        void parse() throws Exception {
            assertThat(json.parseObject("{\"name\":\"Kawa\",\"price\":24.90,\"extra\":true}"))
                    .isEqualTo(new ProductRequest("Kawa", new BigDecimal("24.90")));
            assertThat(json.parseObject("{\"name\":\"Kawa\"}").price()).isNull();
        }
    }
}
