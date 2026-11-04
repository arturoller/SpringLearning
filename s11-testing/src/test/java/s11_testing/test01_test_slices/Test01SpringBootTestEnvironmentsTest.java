package s11_testing.test01_test_slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcja 3 lekcji Test01TestSlices: trzy odmiany {@code @SpringBootTest} (MOCK, RANDOM_PORT, NONE) w jednym pliku.
 * Wszystkie ładują PEŁNĄ aplikację: kontroler, serwis, repozytorium, Flyway, prawdziwą bazę H2 — różnią się tylko
 * tym, czy i jak działa warstwa webowa. Klasy zagnieżdżone są {@code @Nested} i dziedziczą konfigurację tylko tam,
 * gdzie ją same podają.
 */
class Test01SpringBootTestEnvironmentsTest {

    /** MOCK (domyślne): pełny kontekst, ale BEZ prawdziwego serwera; zapytania idą przez MockMvc. */
    @Nested
    @SpringBootTest   // webEnvironment = MOCK jest wartością domyślną
    @AutoConfigureMockMvc   // bez tej adnotacji MockMvc nie jest tworzony w @SpringBootTest
    @ActiveProfiles("test01")
    class MockEnvironment {

        @Autowired
        ApplicationContext context;

        @Autowired
        MockMvc mockMvc;

        @Test
        @DisplayName("Sekcja 3: MOCK — pełny kontekst (serwis, repozytorium), kontekst webowy bez serwera, odpowiedź z bazy")
        void fullContextWithoutServer() throws Exception {
            assertThat(context).isInstanceOf(WebApplicationContext.class);
            assertThat(context.getBeanNamesForType(ProductService.class)).hasSize(1);
            assertThat(context.getBeanNamesForType(ProductRepository.class)).hasSize(1);

            mockMvc.perform(get("/test01/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Kawa"));
        }
    }

    /** RANDOM_PORT: prawdziwy Tomcat na losowym porcie; zapytania idą przez sieć lokalną. */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("test01")
    class RandomPort {

        @LocalServerPort
        int port;   // port wybrany przez system

        @Test
        @DisplayName("Sekcja 3: RANDOM_PORT — RestClient łączy się z prawdziwym serwerem na losowym porcie")
        void realServer() {
            RestClient client = RestClient.create("http://localhost:" + port);

            assertThat(port).isPositive();
            assertThat(client.get().uri("/test01/products/2").retrieve().body(ProductResponse.class).name())
                    .isEqualTo("Herbata");
        }

        @Test
        @DisplayName("Sekcja 3: RestClient zamienia odpowiedź 404 na wyjątek HttpClientErrorException.NotFound")
        void restClientThrowsOn404() {
            RestClient client = RestClient.create("http://localhost:" + port);

            assertThatThrownBy(() -> client.get().uri("/test01/products/99").retrieve().body(String.class))
                    .isInstanceOf(HttpClientErrorException.NotFound.class)
                    .hasMessageContaining("Nie ma produktu o numerze 99");
        }
    }

    /** NONE: pełny kontekst bez warstwy webowej — dla aplikacji bez HTTP albo testów samego serwisu z bazą. */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @ActiveProfiles("test01")
    class NoWebEnvironment {

        @Autowired
        ApplicationContext context;

        @Autowired
        ProductService service;

        @Test
        @DisplayName("Sekcja 3: NONE — kontekst nie jest webowy, nie ma DispatcherServlet, serwis działa na prawdziwej bazie")
        void noWeb() {
            assertThat(context).isNotInstanceOf(GenericWebApplicationContext.class);
            assertThat(context.getBeanNamesForType(DispatcherServlet.class)).isEmpty();
            assertThat(service.get(1).getName()).isEqualTo("Kawa");
        }
    }
}
