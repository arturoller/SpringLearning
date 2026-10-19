package s02_configuration.cfg04_yaml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.InvalidConfigDataPropertyException;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Cfg04Yaml. {@code spring.config.name=cfg04} w {@code @SpringBootTest(properties = ...)} każe Bootowi
 * czytać cfg04.yml i cfg04.properties zamiast application.* (nazwa konfiguracji musi być znana przed wczytaniem plików,
 * dlatego podajemy ją tu, a nie w pliku). Do pułapek składni używamy YamlPropertySourceLoader — tej samej ładowarki,
 * której używa Boot — na tekście zapisanym w teście. Gdy trzeba pokazać zachowanie całej aplikacji, tekst YAML trafia
 * do pliku w katalogu tymczasowym ({@code @TempDir} — JUnit tworzy go i usuwa sam).
 */
@SpringBootTest(properties = "spring.config.name=cfg04")
class Cfg04YamlTest {

    @Autowired
    YamlShopProperties shop;

    @Autowired
    Environment environment;

    /** Ładuje tekst YAML tak samo jak Boot i zwraca źródła ustawień (po jednym na dokument). */
    private static List<PropertySource<?>> load(String yaml) throws IOException {
        return new YamlPropertySourceLoader().load("test", new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String[] keys(PropertySource<?> source) {
        return ((EnumerablePropertySource<?>) source).getPropertyNames();
    }

    /**
     * Zapisuje tekst YAML do pliku w katalogu tymczasowym testu i startuje lekcję TYLKO z tym plikiem
     * (spring.config.location = położenie konfiguracji; zastępuje domyślne miejsca, więc cfg04.yml nie jest czytany).
     */
    private static ConfigurableApplicationContext startWithYaml(Path dir, String yaml, String... extraArgs)
            throws IOException {
        Path file = Files.writeString(dir.resolve("test.yml"), yaml, StandardCharsets.UTF_8);
        List<String> args = new ArrayList<>(List.of("--spring.config.location=file:" + file.toAbsolutePath()));
        args.addAll(List.of(extraArgs));
        return new SpringApplicationBuilder(Cfg04Application.class)
                .web(WebApplicationType.NONE)
                .run(args.toArray(new String[0]));
    }

    @Test
    @DisplayName("1. YAML i .properties to te same klucze: hierarchia z wcięć jest spłaszczana do kropek, a polskie litery działają")
    void yamlIsFlattenedToDottedKeys() throws IOException {
        assertThat(shop.name()).isEqualTo("Sklep YAML");
        assertThat(shop.city()).isEqualTo("Łódź");                  // YAML czytany jako UTF-8
        assertThat(shop.address().street()).isEqualTo("Długa 1");
        assertThat(shop.address().zip()).isEqualTo("90-001");
        // ten sam klucz w postaci "properties" — tak widzi go Environment
        assertThat(environment.getProperty("cfg04.shop.address.street")).isEqualTo("Długa 1");

        // jak YAML wygląda po spłaszczeniu
        PropertySource<?> flat = load("""
                cfg04:
                  shop:
                    name: X
                    days:
                      - pon
                      - wt
                """).get(0);
        assertThat(keys(flat)).containsExactlyInAnyOrder("cfg04.shop.name", "cfg04.shop.days[0]", "cfg04.shop.days[1]");
    }

    @Test
    @DisplayName("2. Lista w YAML (pozycje z myślnikiem) wiąże się z List<String>, tak jak lista po przecinku w .properties")
    void yamlList() {
        assertThat(shop.openingDays()).containsExactly("pon", "wt", "śr");
        assertThat(environment.getProperty("cfg04.shop.opening-days[2]")).isEqualTo("śr");
    }

    @Test
    @DisplayName("3. Wiele dokumentów w jednym pliku: drugi dokument działa tylko przy profilu z on-profile")
    void multiDocument(@TempDir Path dir) throws IOException {
        // bez profilu: baner z drugiego dokumentu nie jest wczytany (zostaje @DefaultValue)
        assertThat(shop.banner()).isEqualTo("brak");

        // z profilem cfg04-promo: drugi dokument się włącza
        try (ConfigurableApplicationContext promo = new SpringApplicationBuilder(Cfg04Application.class)
                .web(WebApplicationType.NONE)
                .run("--spring.config.name=cfg04", "--spring.profiles.active=cfg04-promo")) {
            assertThat(promo.getBean(YamlShopProperties.class).banner()).isEqualTo("Wyprzedaż do -30%!");
        }

        // ładowarka dzieli plik na dokumenty (separator ---), a warunek jest zwykłym kluczem
        List<PropertySource<?>> documents = load("""
                a: 1
                ---
                spring:
                  config:
                    activate:
                      on-profile: promo
                a: 2
                """);
        assertThat(documents).hasSize(2);
        assertThat(documents.get(1).getProperty("spring.config.activate.on-profile")).isEqualTo("promo");

        // PUŁAPKA a) brak --- i powtórzony klucz główny (cfg04:) — zdublowany klucz w jednym dokumencie = błąd startu
        assertThatThrownBy(() -> load("""
                cfg04:
                  shop:
                    name: Sklep YAML
                spring:
                  config:
                    activate:
                      on-profile: cfg04-promo
                cfg04:
                  shop:
                    banner: Promocja
                """)).hasStackTraceContaining("found duplicate key cfg04");

        // PUŁAPKA b) brak --- bez powtórzonych kluczy — JEDEN dokument, a warunek on-profile obejmuje go CAŁEGO
        String noSeparator = """
                cfg04:
                  shop:
                    name: Sklep YAML
                spring:
                  config:
                    activate:
                      on-profile: cfg04-promo
                """;
        try (ConfigurableApplicationContext context = startWithYaml(dir, noSeparator)) {
            assertThat(context.getEnvironment().getProperty("cfg04.shop.name")).isNull();   // „zwykła” nazwa zniknęła!
        }
        try (ConfigurableApplicationContext context = startWithYaml(dir, noSeparator, "--spring.profiles.active=cfg04-promo")) {
            assertThat(context.getEnvironment().getProperty("cfg04.shop.name")).isEqualTo("Sklep YAML");
        }

        // stary zapis spring.profiles: w Boot 4 aplikacja nie startuje
        assertThatThrownBy(() -> startWithYaml(dir, """
                cfg04:
                  shop:
                    name: Sklep YAML
                ---
                spring:
                  profiles: cfg04-promo
                cfg04:
                  shop:
                    banner: Promocja
                """))
                .isInstanceOf(InvalidConfigDataPropertyException.class)
                .hasMessageContaining("should be replaced with 'spring.config.activate.on-profile'");
    }

    @Test
    @DisplayName("4. PUŁAPKA: ten sam klucz w .properties i .yml — wygrywa .properties")
    void propertiesBeatYaml() {
        assertThat(shop.conflict()).isEqualTo("z-properties");
    }

    @Test
    @DisplayName("5. PUŁAPKA: tabulator we wcięciu to błąd, zła głębokość wcięcia cicho zmienia klucze")
    void indentationPitfalls(@TempDir Path dir) throws IOException {
        // tabulator zamiast spacji: błąd składni (ScannerException z biblioteki SnakeYAML)
        assertThatThrownBy(() -> load("cfg04:\n\tname: X\n"))
                .hasStackTraceContaining("found character '\\t(TAB)' that cannot start any token")
                .hasStackTraceContaining("(Do not use \\t(TAB) for indentation)");

        // za małe wcięcie: YAML jest poprawny, ale klucz ląduje piętro wyżej — i nikt nie zgłasza błędu
        String wrongIndent = """
                cfg04:
                  shop:
                  name: X
                """;
        PropertySource<?> wrong = load(wrongIndent).get(0);
        assertThat(keys(wrong)).contains("cfg04.name").doesNotContain("cfg04.shop.name");
        // cała aplikacja startuje bez błędu, a pole name rekordu zostaje null
        try (ConfigurableApplicationContext context = startWithYaml(dir, wrongIndent)) {
            assertThat(context.getBean(YamlShopProperties.class).name()).isNull();
        }
    }

    @Test
    @DisplayName("6. PUŁAPKA: NO, yes, on w YAML-u to wartości logiczne, nie tekst — w razie wątpliwości używaj cudzysłowu")
    void yamlBooleanTrap() throws IOException {
        // w naszym pliku country: NO  →  tekst "false"
        assertThat(shop.country()).isEqualTo("false");

        PropertySource<?> source = load("a: NO\nb: \"NO\"\nc: yes\nd: 1.10\ne: \"1.10\"\n").get(0);
        assertThat(String.valueOf(source.getProperty("d"))).isEqualTo("1.1");     // liczba: zero na końcu przepadło
        assertThat(source.getProperty("e")).isEqualTo("1.10");                    // w cudzysłowie: tekst bez zmian
        assertThat(source.getProperty("a")).isEqualTo(Boolean.FALSE);
        assertThat(source.getProperty("b")).isEqualTo("NO");
        assertThat(source.getProperty("c")).isEqualTo(Boolean.TRUE);

        // tylko trzy zapisy są wartościami logicznymi: małe litery, z wielkiej litery, WIELKIE; reszta to tekst
        PropertySource<?> cases = load("a: No\nb: ON\nc: Off\nd: nO\ne: oFF\nf: y\ng: N\n").get(0);
        assertThat(cases.getProperty("a")).isEqualTo(Boolean.FALSE);
        assertThat(cases.getProperty("b")).isEqualTo(Boolean.TRUE);
        assertThat(cases.getProperty("c")).isEqualTo(Boolean.FALSE);
        assertThat(cases.getProperty("d")).isEqualTo("nO");
        assertThat(cases.getProperty("e")).isEqualTo("oFF");
        assertThat(cases.getProperty("f")).isEqualTo("y");
        assertThat(cases.getProperty("g")).isEqualTo("N");
    }
}
