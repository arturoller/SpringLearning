package s02_configuration.cfg04_yaml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Cfg04Yaml. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Cfg04YamlExercisesTest {

    private static List<PropertySource<?>> load(String yaml) throws IOException {
        return new YamlPropertySourceLoader().load("cwiczenie", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String text(PropertySource<?> source, String key) {
        return String.valueOf(source.getProperty(key));
    }

    @Test
    @DisplayName("Ćwiczenie 1: ustawienia .properties zapisane jako YAML")
    void exercise1() throws IOException {
        List<PropertySource<?>> documents = load(Cfg04Yaml.exercise1());
        assertThat(documents).hasSize(1);
        PropertySource<?> source = documents.get(0);
        assertThat(text(source, "cfg04.exercise.name")).isEqualTo("Kawiarnia");
        assertThat(text(source, "cfg04.exercise.tables")).isEqualTo("12");
        assertThat(text(source, "cfg04.exercise.open")).isEqualTo("true");
        assertThat(text(source, "cfg04.exercise.menu[0]")).isEqualTo("kawa");
        assertThat(text(source, "cfg04.exercise.menu[1]")).isEqualTo("herbata");
    }

    @Test
    @DisplayName("Ćwiczenie 2: dwa dokumenty, drugi tylko dla profilu cfg04-promo")
    void exercise2() throws IOException {
        List<PropertySource<?>> documents = load(Cfg04Yaml.exercise2());
        assertThat(documents).hasSize(2);
        assertThat(text(documents.get(0), "cfg04.exercise.greeting")).isEqualTo("Witaj");
        assertThat(text(documents.get(1), "spring.config.activate.on-profile")).isEqualTo("cfg04-promo");
        assertThat(text(documents.get(1), "cfg04.exercise.greeting")).isEqualTo("Promocja!");
    }

    @Test
    @DisplayName("Ćwiczenie 3: poprawione wcięcia dają klucze cfg04.exercise.db.*")
    void exercise3() throws IOException {
        PropertySource<?> source = load(Cfg04Yaml.exercise3()).get(0);
        assertThat(text(source, "cfg04.exercise.db.url")).isEqualTo("jdbc:h2:mem:cfg04");
        assertThat(text(source, "cfg04.exercise.db.user")).isEqualTo("sa");
        assertThat(text(source, "cfg04.exercise.db.pool.max")).isEqualTo("5");
    }
}
