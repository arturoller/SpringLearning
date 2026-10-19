package s03_boot_basics.solutions.boot01_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Boot01AutoConfigurationExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Metoda solution3 to wzorcowe rozwiązanie ćwiczenia 3 (ćwiczenie polega na napisaniu testu).
 */
@Tag("wzorzec")
class Boot01AutoConfigurationSolutionsTest {

    /** Rekord jak w lekcji — rozwiązanie nie korzysta z klas szkieletu ćwiczeń. */
    record CustomerJson(long id, String name, String email) {
    }

    /** Konfiguracja użytkownika do rozwiązania 3 (bez @Configuration — nie trafi do skanowania). */
    static class ShopMapperConfig {
        @Bean
        JsonMapper shopMapper() {
            return JsonMapper.builder().build();
        }
    }

    private final ApplicationContextRunner jackson = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @Test
    @DisplayName("Wzorzec 1: jedno ustawienie spring.jackson.* sprawia, że pola null znikają z JSON-a")
    void solution1() {
        jackson.withPropertyValues(Boot01AutoConfigurationSolutions.solution1()).run(ctx -> {
            JsonMapper mapper = ctx.getBean(JsonMapper.class);
            assertThat(mapper.writeValueAsString(new CustomerJson(2, "Maria Nowak", null)))
                    .isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\"}");
            assertThat(mapper.writeValueAsString(new CustomerJson(1, "Jan Kowalski", "jan@example.com")))
                    .isEqualTo("{\"id\":1,\"name\":\"Jan Kowalski\",\"email\":\"jan@example.com\"}");
        });
        // dla porównania: bez ustawienia pole null JEST zapisywane
        jackson.run(ctx -> assertThat(ctx.getBean(JsonMapper.class).writeValueAsString(new CustomerJson(2, "Maria Nowak", null)))
                .isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\",\"email\":null}"));
    }

    @Test
    @DisplayName("Wzorzec 2: aplikacja ćwiczenia bez Jacksona, a lista wykluczeń pochodzi z raportu warunków")
    void solution2() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Boot01AutoConfigurationSolutions.Exercise2Application.class)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false).run()) {
            assertThat(Boot01AutoConfigurationSolutions.solution2Exclusions(ctx))
                    .containsExactly("org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration");
            assertThat(ctx.getBeanNamesForType(JsonMapper.class)).isEmpty();
        }
    }

    @Test
    @DisplayName("Wzorzec 3: własny JsonMapper wyłącza bean Boota, a raport warunków mówi dlaczego")
    void solution3() {
        jackson.withUserConfiguration(ShopMapperConfig.class).run(ctx -> {
            // (a) w kontekście jest dokładnie jeden JsonMapper — Twój
            assertThat(ctx).hasSingleBean(JsonMapper.class);
            assertThat(ctx.getBean(JsonMapper.class)).isSameAs(ctx.getBean("shopMapper"));

            // (b) raport: metoda Boota nie przeszła warunku @ConditionalOnMissingBean
            ConditionAndOutcomes outcomes = ConditionEvaluationReport.get(ctx.getBeanFactory())
                    .getConditionAndOutcomesBySource()
                    .get("org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration#jacksonJsonMapper");
            assertThat(outcomes.isFullMatch()).isFalse();
            assertThat(outcomes.stream().map(o -> o.getOutcome().getMessage()))
                    .anyMatch(m -> m.contains("found beans of type 'tools.jackson.databind.json.JsonMapper' shopMapper"));
        });
    }
}
