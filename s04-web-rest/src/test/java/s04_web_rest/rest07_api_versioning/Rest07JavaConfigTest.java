package s04_web_rest.rest07_api_versioning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 3 i 5 lekcji Rest07ApiVersioning: konfiguracja wersjonowania w JAVIE (WebMvcConfigurer.configureApiVersioning)
 * — strategia „wersja w ścieżce” — oraz pułapka „version bez strategii”. Zwykły test JUnit: małe, osobne konteksty
 * (WebApplicationContextRunner), bo strategia ze ścieżki zmieniłaby zasady dla WSZYSTKICH adresów lekcji.
 */
class Rest07JavaConfigTest {

    @Test
    @DisplayName("3. Wersja w ścieżce (usePathSegment(1)): /api/v1/… i /api/1/… → wersja 1, /api/v2/… → 2, /api/v3/… → 400")
    void versionFromPathSegment() {
        new WebApplicationContextRunner()
                .withUserConfiguration(PathVersioningConfiguration.class)
                .run(context -> {
                    MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
                    mockMvc.perform(get("/api/v1/orders/5")).andExpect(content().string("zamówienie 5 (v1)"));
                    mockMvc.perform(get("/api/1/orders/5")).andExpect(content().string("zamówienie 5 (v1)"));
                    mockMvc.perform(get("/api/v2/orders/5")).andExpect(content().string("zamówienie 5 (v2)"));
                    mockMvc.perform(get("/api/v3/orders/5")).andExpect(status().isBadRequest());
                });
    }

    @Test
    @DisplayName("5. PUŁAPKA: atrybut version bez skonfigurowanej strategii → aplikacja NIE startuje")
    void versionWithoutStrategyFailsAtStartup() {
        new WebApplicationContextRunner()
                .withUserConfiguration(NoStrategyConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("API version specified, but no ApiVersionStrategy configured");
                });
    }

    /** Ścieżka /api/{version}/orders: segment 0 = "api", segment 1 = wersja. */
    @RestController
    @RequestMapping("/api/{version}/orders")
    static class PathVersionedOrderController {

        @GetMapping(path = "/{id}", version = "1")
        String v1(@PathVariable long id) {
            return "zamówienie " + id + " (v1)";
        }

        @GetMapping(path = "/{id}", version = "2")
        String v2(@PathVariable long id) {
            return "zamówienie " + id + " (v2)";
        }
    }

    @EnableWebMvc // włącza Spring MVC (w aplikacji Boot robi to autokonfiguracja)
    @Import(PathVersionedOrderController.class)
    static class PathVersioningConfiguration implements WebMvcConfigurer {
        @Override
        public void configureApiVersioning(ApiVersionConfigurer configurer) {
            configurer.usePathSegment(1);   // to samo co spring.mvc.apiversion.use.path-segment=1
        }
    }

    @EnableWebMvc
    @Import(PathVersionedOrderController.class)
    static class NoStrategyConfiguration {
    }
}
