package s11_testing.test03_mock_mvc_tester;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Test03MockMvcTester: w pełnym {@code @SpringBootTest} MockMvcTester też jest, ale dopiero z
 * {@code @AutoConfigureMockMvc} (w wycinku {@code @WebMvcTest} — od razu). Tu zapytanie przechodzi przez PRAWDZIWY
 * serwis i prawdziwe advice z całej aplikacji.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Test03SpringBootTesterTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 1: @SpringBootTest + @AutoConfigureMockMvc — MockMvcTester do wstrzyknięcia, odpowiedź z prawdziwego serwisu")
    void testerInFullContext() {
        assertThat(mvc.get().uri("/test03/products/2"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").asString().isEqualTo("Herbata");
    }
}
