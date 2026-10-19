package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy ĆWICZEŃ lekcji Test01TestSlices. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Każde ćwiczenie to klasa {@code @Nested}: uzupełnij miejsce z TODO i usuń wywołanie {@code todo()}.
 * Asercje (sprawdzenia) na końcu metod są gotowe — nie zmieniaj ich.
 */
@Tag("cwiczenie")
class Test01TestSlicesExercisesTest {

    /** Znacznik „tu jeszcze nic nie ma” — rzuca wyjątek, dopóki go nie usuniesz. */
    static <T> T todo() {
        throw new UnsupportedOperationException("TODO");
    }

    @Nested
    // TODO (ćwiczenie 1): dodaj tu adnotację wycinka z repozytorium i bazą, ale bez kontrolerów.
    class Exercise1 {

        @Autowired
        ApplicationContext context;

        @Autowired
        ProductRepository repository;

        @Test
        @DisplayName("Ćwiczenie 1: wycinek z repozytorium i bazą, bez kontrolera")
        void exercise1() {
            todo();   // TODO: usuń tę linię po dodaniu adnotacji

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
    class Exercise2 {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        ProductService service;

        @Test
        @DisplayName("Ćwiczenie 2: atrapa serwisu zwraca dwa produkty, kontroler oddaje je jako JSON")
        void exercise2() throws Exception {
            // TODO: naucz atrapę service, co ma zwrócić search("kaw") (treść ćwiczenia w klasie lekcji), i usuń todo()
            todo();

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
    @SpringBootTest   // TODO (ćwiczenie 3): zmień środowisko webowe tak, żeby wystartował prawdziwy serwer
    class Exercise3 {

        @Autowired
        ApplicationContext context;

        @Test
        @DisplayName("Ćwiczenie 3: prawdziwe zapytanie HTTP do serwera na losowym porcie")
        void exercise3() throws Exception {
            todo();   // TODO: usuń tę linię po zmianie adnotacji

            assertThat(context).isInstanceOf(WebServerApplicationContext.class);
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();

            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newHttpClient()) {
                response = client.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/products?q=lampka")).build(),
                        HttpResponse.BodyHandlers.ofString());
            }
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("[]");   // baza tej lekcji jest pusta
        }
    }

    @Nested
    @JsonTest
    class Exercise4 {

        @Autowired
        JacksonTester<ProductDto> json;

        @Test
        @DisplayName("Ćwiczenie 4: JSON → ProductDto i ścieżka JSON do ceny w złotych")
        void exercise4() throws Exception {
            var expected = new ProductDto(14L, "Lampka biurkowa", 12900, "129.00");

            String text = todo();   // TODO: tekst JSON, z którego powstanie dokładnie rekord expected
            String path = todo();   // TODO: ścieżka JSON (JsonPath) do ceny w złotych, np. "$.coś"

            assertThat(json.parseObject(text)).isEqualTo(expected);
            assertThat(json.write(expected)).extractingJsonPathStringValue(path).isEqualTo("129.00");
        }
    }
}
