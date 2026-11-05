package s04_web_rest.rest01_controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji: pułapka „Ambiguous mapping”. To zwykły test JUnit (bez @WebMvcTest) — budujemy mały kontekst
 * webowy sami, bo błędny kontroler nie może trafić do kontekstu pozostałych testów lekcji (aplikacja by nie wystartowała).
 */
class Rest01AmbiguousMappingTest {

    @Test
    @DisplayName("5. PUŁAPKA: dwie metody z tym samym mapowaniem → błąd przy starcie (Ambiguous mapping)")
    void ambiguousMappingFailsAtStartup() {
        // WebApplicationContextRunner nie rzuca wyjątku — zapamiętuje błąd startu kontekstu w context.getStartupFailure()
        new WebApplicationContextRunner()
                .withUserConfiguration(AmbiguousConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("Ambiguous mapping");
                });
    }

    @EnableWebMvc // włącza Spring MVC (w aplikacji Boot robi to autokonfiguracja)
    @Import(DuplicateController.class)
    static class AmbiguousConfiguration {
    }

    @RestController
    static class DuplicateController {

        @GetMapping("/dup")
        String first() {
            return "pierwsza";
        }

        @GetMapping("/dup")
        String second() {
            return "druga";
        }
    }
}
