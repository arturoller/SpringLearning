package s04_web_rest.rest06_cors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 3–4 lekcji Rest06Cors: PUŁAPKA {@code allowedOrigins("*")} + {@code allowCredentials(true)}
 * oraz PUŁAPKA {@code @CrossOrigin} bez parametrów.
 * To zwykły test JUnit (bez @WebMvcTest) — błędne konfiguracje budujemy w małych, osobnych kontekstach
 * (WebApplicationContextRunner), żeby nie zepsuły kontekstu pozostałych testów lekcji.
 */
class Rest06CredentialsPitfallTest {

    private static final String MESSAGE = "When allowCredentials is true, allowedOrigins cannot contain the special value \"*\"";

    @Test
    @DisplayName("4. CorsConfiguration: „*” + poświadczenia → IllegalArgumentException z podpowiedzią allowedOriginPatterns")
    void validationMessage() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.setAllowCredentials(true);

        assertThatThrownBy(config::validateAllowCredentials)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MESSAGE)
                .hasMessageContaining("allowedOriginPatterns");
    }

    @Test
    @DisplayName("4. @CrossOrigin(origins = \"*\", allowCredentials = \"true\") → aplikacja NIE startuje")
    void annotationFailsAtStartup() {
        new WebApplicationContextRunner()
                .withUserConfiguration(AnnotatedConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining(MESSAGE);
                });
    }

    @Test
    @DisplayName("4. addCorsMappings z „*” + poświadczenia → start się udaje, ale KAŻDE zapytanie na tę ścieżkę kończy się wyjątkiem")
    void globalMappingFailsAtRequestTime() {
        new WebApplicationContextRunner()
                .withUserConfiguration(GlobalStarConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();   // aplikacja wystartowała — błąd jest „uśpiony”
                    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

                    // Spring sprawdza regułę przy KAŻDYM zapytaniu na ścieżkę objętą CORS — także bez nagłówka Origin
                    assertThatThrownBy(() -> mockMvc.perform(get("/pitfall")))
                            .hasStackTraceContaining(MESSAGE);
                    assertThatThrownBy(() -> mockMvc.perform(get("/pitfall").header("Origin", "https://a.example.com")))
                            .hasStackTraceContaining(MESSAGE);
                });
    }

    @Test
    @DisplayName("4. Naprawa: allowedOriginPatterns(\"*\") + poświadczenia → odpowiedź z KONKRETNYM pochodzeniem, nie z „*”")
    void originPatternsFix() {
        new WebApplicationContextRunner()
                .withUserConfiguration(PatternConfiguration.class)
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
                    mockMvc.perform(get("/pitfall").header("Origin", "https://a.example.com"))
                            .andExpect(status().isOk())
                            .andExpect(header().string("Access-Control-Allow-Origin", "https://a.example.com"))
                            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
                });
    }

    @Test
    @DisplayName("3. PUŁAPKA: @CrossOrigin bez parametrów wpuszcza KAŻDE pochodzenie (Access-Control-Allow-Origin: *)")
    void bareCrossOriginAllowsEveryone() {
        new WebApplicationContextRunner()
                .withUserConfiguration(BareConfiguration.class)
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
                    mockMvc.perform(get("/bare").header("Origin", "https://evil.example.com"))
                            .andExpect(status().isOk())
                            .andExpect(header().string("Access-Control-Allow-Origin", "*"));
                });
    }

    @RestController
    @CrossOrigin
    static class BareController {
        @GetMapping("/bare")
        String bare() {
            return "ok";
        }
    }

    @EnableWebMvc
    @Import(BareController.class)
    static class BareConfiguration {
    }

    @RestController
    static class PitfallController {
        @GetMapping("/pitfall")
        String pitfall() {
            return "ok";
        }
    }

    @RestController
    @CrossOrigin(origins = "*", allowCredentials = "true")
    static class AnnotatedController {
        @GetMapping("/annotated")
        String annotated() {
            return "ok";
        }
    }

    @EnableWebMvc // włącza Spring MVC (w aplikacji Boot robi to autokonfiguracja)
    @Import(AnnotatedController.class)
    static class AnnotatedConfiguration {
    }

    @EnableWebMvc
    @Import(PitfallController.class)
    static class GlobalStarConfiguration implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**").allowedOrigins("*").allowCredentials(true);
        }
    }

    @EnableWebMvc
    @Import(PitfallController.class)
    static class PatternConfiguration implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**").allowedOriginPatterns("*").allowCredentials(true);
        }
    }
}
