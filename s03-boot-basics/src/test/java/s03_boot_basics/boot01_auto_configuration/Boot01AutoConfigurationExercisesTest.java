package s03_boot_basics.boot01_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Boot01AutoConfiguration. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Boot01AutoConfigurationExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: jedno ustawienie spring.jackson.* sprawia, że pola null znikają z JSON-a")
    void exercise1() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues(Boot01AutoConfiguration.exercise1())
                .run(ctx -> {
                    JsonMapper mapper = ctx.getBean(JsonMapper.class);
                    assertThat(mapper.writeValueAsString(new Boot01AutoConfiguration.CustomerJson(2, "Maria Nowak", null)))
                            .isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\"}");
                    assertThat(mapper.writeValueAsString(
                            new Boot01AutoConfiguration.CustomerJson(1, "Jan Kowalski", "jan@example.com")))
                            .isEqualTo("{\"id\":1,\"name\":\"Jan Kowalski\",\"email\":\"jan@example.com\"}");
                });
    }

    @Test
    @DisplayName("Ćwiczenie 2: aplikacja ćwiczenia bez Jacksona, a lista wykluczeń pochodzi z raportu warunków")
    void exercise2() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Boot01AutoConfiguration.Exercise2Application.class)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false).run()) {
            assertThat(Boot01AutoConfiguration.exercise2Exclusions(ctx))
                    .containsExactly("org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration");
            assertThat(ctx.getBeanNamesForType(JsonMapper.class)).as("JsonMapper nie powinien istnieć").isEmpty();
        }
    }

    @Test
    @DisplayName("Ćwiczenie 3: Twój test — własny JsonMapper wyłącza bean Boota, a raport warunków mówi dlaczego")
    void exercise3() {
        // TODO: twoje rozwiązanie — opis w Boot01AutoConfiguration (ĆWICZENIE 3). Usuń poniższą linię.
        throw new UnsupportedOperationException("TODO");
    }
}
