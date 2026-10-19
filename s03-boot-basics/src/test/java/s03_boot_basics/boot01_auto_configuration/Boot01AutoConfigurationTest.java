package s03_boot_basics.boot01_auto_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Boot01AutoConfiguration — każdy test to „wynik” jednej sekcji.
 *
 * <p>Dwa rodzaje kontekstu:</p>
 * <ul>
 *   <li>{@code @SpringBootTest} — prawdziwa aplikacja lekcji (Boot01Application) ze wszystkimi autokonfiguracjami
 *       modułu. Potrzebna do raportu warunków „całej aplikacji”.</li>
 *   <li>{@code ApplicationContextRunner} (uruchamiacz kontekstu) — malutki kontekst z JEDNĄ autokonfiguracją,
 *       budowany w ułamku sekundy. Idealny do sprawdzania warunków: z beanem / bez beana / z ustawieniem / bez klasy.</li>
 * </ul>
 * Klasy konfiguracji w tym teście celowo NIE mają {@code @Configuration}: wtedy skanowanie pakietu lekcji
 * (np. przy starcie Boot01Application w sekcji 5) ich nie znajdzie, a {@code withUserConfiguration} i tak
 * przetworzy ich metody {@code @Bean}.
 */
@SpringBootTest
class Boot01AutoConfigurationTest {

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    ProductJsonService productJsonService;

    /** Lekki kontekst: tylko autokonfiguracja Jacksona. */
    private final ApplicationContextRunner jackson = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    /** Własny bean TEGO SAMEGO typu, którego pilnuje {@code @ConditionalOnMissingBean} w Boocie. */
    static class OwnJsonMapperConfig {
        @Bean
        JsonMapper shopMapper() {
            return JsonMapper.builder().build();
        }
    }

    /** Własny bean INNEGO typu (nadklasa ObjectMapper) — PUŁAPKA z sekcji 6. */
    static class OwnObjectMapperConfig {
        @Bean
        ObjectMapper myObjectMapper() {
            return new ObjectMapper();
        }
    }

    /** Aplikacja „tylko autokonfiguracja”: bez skanowania pakietu (brak @Configuration, więc też nie zostanie zeskanowana). */
    @EnableAutoConfiguration
    static class AutoConfigurationOnly {
    }

    /** To samo, ale z wyłączeniem w adnotacji — jak {@code @SpringBootApplication(exclude = …)}. */
    @EnableAutoConfiguration(exclude = JacksonAutoConfiguration.class)
    static class AutoConfigurationWithoutJackson {
    }

    private static ConfigurableApplicationContext start(Class<?> source, String... args) {
        return new SpringApplicationBuilder(source)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(args);
    }

    private static List<String> messages(ConditionAndOutcomes outcomes) {
        return outcomes.stream().map(o -> o.getOutcome().getMessage()).toList();
    }

    @Test
    @DisplayName("1. @SpringBootApplication = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan z dwoma filtrami")
    void springBootApplicationIsAComposedAnnotation() {
        assertThat(AnnotatedElementUtils.hasAnnotation(Boot01Application.class, SpringBootConfiguration.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(Boot01Application.class, Configuration.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(Boot01Application.class, EnableAutoConfiguration.class)).isTrue();

        ComponentScan scan = AnnotatedElementUtils.findMergedAnnotation(Boot01Application.class, ComponentScan.class);
        assertThat(scan).isNotNull();
        assertThat(Arrays.stream(scan.excludeFilters()).flatMap(filter -> Arrays.stream(filter.classes())))
                .containsExactly(TypeExcludeFilter.class, AutoConfigurationExcludeFilter.class);
    }

    @Test
    @DisplayName("2. Lista autokonfiguracji pochodzi z plików AutoConfiguration.imports w wielu jarach (Boot 4: moduły)")
    void autoConfigurationCandidatesComeFromImportsFiles() throws IOException {
        List<String> candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())
                .getCandidates();
        assertThat(candidates).contains(
                "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration",
                "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration");
        // w tym module nie ma żadnej autokonfiguracji webowej — nie ma jej jaru na ścieżce klas
        assertThat(candidates).noneMatch(name -> name.contains(".web"));

        List<String> files = Collections.list(getClass().getClassLoader()
                        .getResources("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"))
                .stream().map(URL::toString).toList();
        assertThat(files).anyMatch(url -> url.contains("spring-boot-autoconfigure-"));
        assertThat(files).anyMatch(url -> url.contains("spring-boot-jackson-"));

        // Boot 3 miał tę klasę w spring-boot-autoconfigure; w Boot 4 przeniesiono ją do modułu spring-boot-jackson
        assertThat(ClassUtils.isPresent("org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration", null))
                .isFalse();
    }

    @Test
    @DisplayName("3. Bez Twojego beana Boot tworzy JsonMapper; Twój JsonMapper wygrywa — autokonfiguracja się wycofuje")
    void autoConfigurationBacksOff() {
        // pełna aplikacja: serwis dostał mapper od Boota
        assertThat(productJsonService.mapper()).isSameAs(context.getBean("jacksonJsonMapper"));
        assertThat(productJsonService.toJson(new Product("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"))))
                .isEqualTo("{\"sku\":\"ELE-001\",\"name\":\"Laptop Pro 14\",\"price\":5499.99}");

        jackson.run(ctx -> {
            assertThat(ctx).hasSingleBean(JsonMapper.class);
            assertThat(ctx).hasBean("jacksonJsonMapper");
            assertThat(ctx.getBeanFactory().getBeanDefinition("jacksonJsonMapper").isPrimary()).isTrue();
            assertThat(ctx.getBeanFactory().getBeanDefinition("jsonMapperBuilder").getScope()).isEqualTo("prototype");
        });

        jackson.withUserConfiguration(OwnJsonMapperConfig.class).run(ctx -> {
            assertThat(ctx).hasSingleBean(JsonMapper.class);
            assertThat(ctx).doesNotHaveBean("jacksonJsonMapper");
            assertThat(ctx.getBean(JsonMapper.class)).isSameAs(ctx.getBean("shopMapper"));
        });

        // cena za własny mapper zbudowany od zera: ustawienia spring.jackson.* go nie dotyczą (nie przeszedł przez Boota)
        jackson.withUserConfiguration(OwnJsonMapperConfig.class)
                .withPropertyValues("spring.jackson.serialization.indent-output=true")
                .run(ctx -> assertThat(ctx.getBean(JsonMapper.class).writeValueAsString(Map.of("sku", "ELE-001")))
                        .isEqualTo("{\"sku\":\"ELE-001\"}"));
    }

    @Test
    @DisplayName("4. Warunki: ustawienie spring.jackson.* zmienia mapper, brak klasy wyłącza autokonfigurację, brak weba wyłącza WebHomePage")
    void conditions() {
        jackson.withPropertyValues("spring.jackson.serialization.indent-output=true").run(ctx ->
                assertThat(ctx.getBean(JsonMapper.class).writeValueAsString(Map.of("sku", "ELE-001")))
                        .isEqualToNormalizingNewlines("{\n  \"sku\" : \"ELE-001\"\n}"));

        // @ConditionalOnClass(JsonMapper.class): udajemy, że Jacksona nie ma na ścieżce klas
        jackson.withClassLoader(new FilteredClassLoader(JsonMapper.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(JsonMapper.class));

        // @ConditionalOnWebApplication na Twoim komponencie: aplikacja nie jest webowa → brak beana
        assertThat(context.getBeansOfType(WebHomePage.class)).isEmpty();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("5. Raport warunków (w kodzie i po --debug) odpowiada na pytanie „dlaczego bean jest / dlaczego go nie ma”")
    void conditionEvaluationReport(CapturedOutput output) {
        Map<String, ConditionAndOutcomes> report =
                ConditionEvaluationReport.get(context.getBeanFactory()).getConditionAndOutcomesBySource();

        String jacksonName = JacksonAutoConfiguration.class.getName();
        assertThat(report.get(jacksonName).isFullMatch()).isTrue();
        assertThat(messages(report.get(jacksonName)))
                .containsExactly("@ConditionalOnClass found required class 'tools.jackson.databind.json.JsonMapper'");
        assertThat(report.get(jacksonName + "#jacksonJsonMapper").isFullMatch()).isTrue();

        // część Jacksona dla ProblemDetail potrzebuje klasy z modułu webowego (spring-web) — tu jej nie ma
        ConditionAndOutcomes problemDetails = report.get(jacksonName + "$JsonProblemDetailsConfiguration");
        assertThat(problemDetails.isFullMatch()).isFalse();
        assertThat(messages(problemDetails))
                .containsExactly("@ConditionalOnClass did not find required class 'org.springframework.http.ProblemDetail'");

        // Twój komponent z warunkiem też trafia do raportu
        ConditionAndOutcomes web = report.get(WebHomePage.class.getName());
        assertThat(web.isFullMatch()).isFalse();
        assertThat(messages(web)).containsExactly("@ConditionalOnWebApplication (required) did not find servlet web "
                + "application classes and did not find reactive web application classes");

        // Boot sam używa @ConditionalOnBooleanProperty: JMX jest domyślnie wyłączony. Uwaga: @SpringBootTest dodatkowo
        // ustawia spring.jmx.enabled=false, dlatego tu komunikat mówi „found different value”, a przy zwykłym starcie
        // (niżej, --debug) — „did not find property 'spring.jmx.enabled'”.
        ConditionAndOutcomes jmx = report.get("org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration");
        assertThat(jmx.isFullMatch()).isFalse();
        assertThat(messages(jmx)).contains("@ConditionalOnBooleanProperty (spring.jmx.enabled=true) found different "
                + "value in property 'spring.jmx.enabled'");

        // ten sam raport jako tekst: argument --debug (albo debug=true w pliku) wypisuje go przy starcie
        try (ConfigurableApplicationContext ctx = start(Boot01Application.class, "--debug")) {
            assertThat(ctx.isActive()).isTrue();
        }
        assertThat(output).contains("CONDITIONS EVALUATION REPORT", "Positive matches:", "Negative matches:",
                "Exclusions:", "Unconditional classes:", "JacksonAutoConfiguration matched:",
                "JacksonAutoConfiguration.JsonProblemDetailsConfiguration:",
                "@ConditionalOnBooleanProperty (spring.jmx.enabled=true) did not find property 'spring.jmx.enabled'");
    }

    @Test
    @DisplayName("6. exclude i spring.autoconfigure.exclude wyłączają autokonfigurację; nazwa z Boota 3 — cisza, typ beana — błąd")
    void exclusions() {
        try (ConfigurableApplicationContext ctx = start(AutoConfigurationWithoutJackson.class)) {
            assertThat(ctx.getBeanNamesForType(JsonMapper.class)).isEmpty();
            assertThat(ConditionEvaluationReport.get(ctx.getBeanFactory()).getExclusions())
                    .containsExactly(JacksonAutoConfiguration.class.getName());
        }
        try (ConfigurableApplicationContext ctx = start(AutoConfigurationOnly.class,
                "--spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")) {
            assertThat(ctx.getBeanNamesForType(JsonMapper.class)).isEmpty();
        }

        // Co się stanie, gdy wyłączysz Jacksona w aplikacji, której serwis POTRZEBUJE JsonMapper? Start się nie uda.
        assertThatThrownBy(() -> start(Boot01Application.class,
                "--spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"))
                .isInstanceOf(UnsatisfiedDependencyException.class)
                .hasMessageContaining("No qualifying bean of type 'tools.jackson.databind.json.JsonMapper' available");

        // PUŁAPKA: stara nazwa z Boota 3: takiej klasy nie ma na ścieżce klas → Boot ją po cichu pomija, Jackson działa dalej
        try (ConfigurableApplicationContext ctx = start(AutoConfigurationOnly.class,
                "--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration")) {
            assertThat(ctx.getBeanNamesForType(JsonMapper.class)).containsExactly("jacksonJsonMapper");
        }
        // PUŁAPKA: klasa istnieje, ale to nie autokonfiguracja (typ beana zamiast klasy autokonfiguracji) → błąd startu
        assertThatThrownBy(() -> start(AutoConfigurationOnly.class,
                "--spring.autoconfigure.exclude=tools.jackson.databind.json.JsonMapper"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("The following classes could not be excluded because they are not auto-configuration classes:")
                .hasMessageContaining("- tools.jackson.databind.json.JsonMapper");
    }

    @Test
    @DisplayName("7. PUŁAPKA: własny ObjectMapper (inny typ niż JsonMapper) NIE wyłącza autokonfiguracji — wstrzykniesz mapper Boota")
    void beanOfDifferentTypeDoesNotBackOff() {
        jackson.withUserConfiguration(OwnObjectMapperConfig.class).run(ctx -> {
            assertThat(ctx.getBeansOfType(ObjectMapper.class)).containsOnlyKeys("myObjectMapper", "jacksonJsonMapper");
            // mapper Boota ma @Primary, więc to ON trafia tam, gdzie wstrzykujesz ObjectMapper
            assertThat(ctx.getBean(ObjectMapper.class)).isSameAs(ctx.getBean("jacksonJsonMapper"));
            assertThat(ctx.getBean(ObjectMapper.class)).isNotSameAs(ctx.getBean("myObjectMapper"));
        });
    }
}
