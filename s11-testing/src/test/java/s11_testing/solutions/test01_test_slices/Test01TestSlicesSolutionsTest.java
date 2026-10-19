package s11_testing.solutions.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import s11_testing.test01_test_slices.Product;
import s11_testing.test01_test_slices.ProductController;
import s11_testing.test01_test_slices.ProductDto;
import s11_testing.test01_test_slices.ProductRepository;
import s11_testing.test01_test_slices.ProductService;
import s11_testing.test01_test_slices.Test01Application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Test01TestSlices (te same testy co w Test01TestSlicesExercisesTest,
 * już uzupełnione). {@code @Tag("wzorzec")} — biegną zawsze. Zajrzyj dopiero po własnej próbie!
 *
 * <p>Różnica techniczna: ten pakiet ({@code solutions…}) nie leży pod pakietem lekcji, więc test NIE znajdzie sam
 * klasy {@code Test01Application} (szuka w górę drzewa pakietów). Wskazujemy ją jawnie:
 * {@code @ContextConfiguration(classes = …)} w wycinkach albo {@code @SpringBootTest(classes = …)}.</p>
 */
@Tag("wzorzec")
class Test01TestSlicesSolutionsTest {

    @Nested
    @DataJpaTest   // rozwiązanie 1: wycinek JPA — repozytorium i baza są, kontrolerów nie ma
    @ContextConfiguration(classes = Test01Application.class)
    class Solution1 {

        @Autowired
        ApplicationContext context;

        @Autowired
        ProductRepository repository;

        @Test
        @DisplayName("Wzorzec 1: wycinek z repozytorium i bazą, bez kontrolera")
        void solution1() {
            repository.save(new Product("Czekolada gorzka", 749));
            repository.save(new Product("Kawa ziarnista 1kg", 6499));

            assertThat(repository.findByNameContainingIgnoreCaseOrderByNameAsc("czek"))
                    .extracting(Product::getName)
                    .containsExactly("Czekolada gorzka");
            assertThat(context.getBeanNamesForType(ProductController.class)).isEmpty();
        }
    }

    @Nested
    @WebMvcTest(ProductController.class)
    @ContextConfiguration(classes = Test01Application.class)
    class Solution2 {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        ProductService service;

        @Test
        @DisplayName("Wzorzec 2: atrapa serwisu zwraca dwa produkty, kontroler oddaje je jako JSON")
        void solution2() throws Exception {
            given(service.search("kaw")).willReturn(List.of(
                    new ProductDto(13L, "Ekspres do kawy", 189900, "1899.00"),
                    new ProductDto(5L, "Kawa ziarnista 1kg", 6499, "64.99")));

            mockMvc.perform(get("/api/products").param("q", "kaw"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Ekspres do kawy"))
                    .andExpect(jsonPath("$[0].priceZl").value("1899.00"))
                    .andExpect(jsonPath("$[1].id").value(5))
                    .andExpect(jsonPath("$[1].priceGrosze").value(6499));
        }
    }

    @Nested
    @SpringBootTest(classes = Test01Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class Solution3 {

        @Autowired
        ApplicationContext context;

        @Test
        @DisplayName("Wzorzec 3: prawdziwe zapytanie HTTP do serwera na losowym porcie")
        void solution3() throws Exception {
            assertThat(context).isInstanceOf(WebServerApplicationContext.class);
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();

            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newHttpClient()) {
                response = client.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/products?q=lampka")).build(),
                        HttpResponse.BodyHandlers.ofString());
            }
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("[]");
        }
    }

    @Nested
    @JsonTest
    @ContextConfiguration(classes = Test01Application.class)
    class Solution4 {

        @Autowired
        JacksonTester<ProductDto> json;

        @Test
        @DisplayName("Wzorzec 4: JSON → ProductDto i ścieżka JSON do ceny w złotych")
        void solution4() throws Exception {
            var expected = new ProductDto(14L, "Lampka biurkowa", 12900, "129.00");

            String text = """
                    {"id": 14, "name": "Lampka biurkowa", "priceGrosze": 12900, "priceZl": "129.00"}
                    """;
            String path = "$.priceZl";

            assertThat(json.parseObject(text)).isEqualTo(expected);
            assertThat(json.write(expected)).extractingJsonPathStringValue(path).isEqualTo("129.00");
        }
    }
}
