package s02_configuration.cfg01_properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Cfg01Properties.
 * {@code @SpringBootTest} startuje Cfg01Application (znajduje ją w tym samym pakiecie), {@code @ActiveProfiles("cfg01")}
 * włącza plik application-cfg01.properties. Właściwość {@code properties = ...} ma WYŻSZY priorytet niż plik
 * i niż właściwość JVM — dowodzi tego sekcja 6.
 */
@SpringBootTest(properties = {"cfg01.shop.max-items=99", "cfg01.shop.vat-percent=19"})
@ActiveProfiles("cfg01")
class Cfg01PropertiesTest {

    @Autowired
    ShopInfo shop;

    /** Właściwość JVM ustawiona PRZED startem kontekstu testu — ma niższy priorytet niż properties w adnotacji. */
    @BeforeAll
    static void setSystemProperty() {
        System.setProperty("cfg01.shop.vat-percent", "7");
    }

    @AfterAll
    static void clearSystemProperty() {
        System.clearProperty("cfg01.shop.vat-percent");
    }

    @Test
    @DisplayName("1+2. @Value wstrzykuje wartości z application-cfg01.properties; plik .properties czytany jest jako ISO-8859-1")
    void valuesFromFile() throws IOException {
        assertThat(shop.name()).isEqualTo("Sklep Spring");

        // PUŁAPKA: tekst zapisany jako UTF-8 jest w .properties czytany jako ISO-8859-1 → krzaczki
        PropertiesPropertySourceLoader loader = new PropertiesPropertySourceLoader();
        byte[] utf8 = "miasto=Łódź".getBytes(StandardCharsets.UTF_8);
        assertThat(loader.load("utf8", new ByteArrayResource(utf8)).get(0).getProperty("miasto")).isNotEqualTo("Łódź");

        // Obejście: kody unicode (ukośnik + u + 4 cyfry szesnastkowe) — tu składane z kawałków, żeby kompilator ich nie ruszył
        String u = "\\" + "u";
        byte[] escaped = ("miasto=" + u + "0141" + u + "00f3d" + u + "017a").getBytes(StandardCharsets.UTF_8);
        assertThat(loader.load("escaped", new ByteArrayResource(escaped)).get(0).getProperty("miasto")).isEqualTo("Łódź");
    }

    @Test
    @DisplayName("3. Brak klucza + wartość domyślna po dwukropku: slogan z kodu; pusta wartość domyślna daje pusty tekst")
    void defaultValues() {
        assertThat(shop.slogan()).isEqualTo("Wszystko dla programisty");

        new ApplicationContextRunner().withUserConfiguration(EmptyDefault.class).run(context ->
                assertThat(context.getBean(EmptyDefault.class).x).isEmpty());
    }

    @Test
    @DisplayName("4. Konwersja typów: "
            + "tekst z pliku to int, boolean i Duration (Duration tylko dzięki Bootowi; bez jednostki = milisekundy)")
    void typeConversion() {
        assertThat(shop.reservationTime()).isEqualTo(Duration.ofSeconds(30));
        assertThat(shop.newsletter()).isTrue();

        // PUŁAPKA: liczba bez jednostki to milisekundy, nie sekundy
        try (ConfigurableApplicationContext context = start("--cfg01.shop.reservation-time=30")) {
            assertThat(context.getBean(ShopInfo.class).reservationTime()).isEqualTo(Duration.ofMillis(30));
        }

        // goły Spring (bez Boota) nie umie zamienić tekstu na Duration — nawet zapisu ISO
        new ApplicationContextRunner().withUserConfiguration(NeedsDuration.class)
                .withPropertyValues("cfg01.demo.time=PT7S")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("no matching editors or conversion strategy found");
                });
    }

    @Test
    @DisplayName("5. Luźne dopasowanie: kebab-case w @Value znajduje klucz zapisany jako camelCase, odwrotnie nie")
    void relaxedBinding() {
        // Zapis kanoniczny w @Value (max-items) widzi warianty camelCase i z podkreślnikiem
        try (ConfigurableApplicationContext context = start("--cfg01.shop.maxItems=7")) {
            assertThat(context.getBean(ShopInfo.class).maxItems()).isEqualTo(7);
        }
        try (ConfigurableApplicationContext context = start("--cfg01.shop.max_items=8")) {
            assertThat(context.getBean(ShopInfo.class).maxItems()).isEqualTo(8);
        }
        // Zapis camelCase w @Value NIE widzi klucza max-items z pliku
        assertThatThrownBy(() -> bootRun(CamelCasePlaceholder.class, "--cfg01.demo.max-items=5"))
                .hasStackTraceContaining("Could not resolve placeholder 'cfg01.demo.maxItems'");
    }

    @Test
    @DisplayName("6. Priorytet: plik < właściwość JVM < argument; properties w teście wygrywa z JVM i plikiem")
    void priorities() {
        // (a) sam plik (profil cfg01)
        try (ConfigurableApplicationContext context = start()) {
            assertThat(context.getBean(ShopInfo.class).name()).isEqualTo("Sklep Spring");
        }
        // (b) właściwość JVM nadpisuje plik
        System.setProperty("cfg01.shop.name", "Z-JVM");
        try {
            try (ConfigurableApplicationContext context = start()) {
                assertThat(context.getBean(ShopInfo.class).name()).isEqualTo("Z-JVM");
            }
            // (c) argument nadpisuje i JVM, i plik
            try (ConfigurableApplicationContext context = start("--cfg01.shop.name=Z-ARGUMENTU")) {
                assertThat(context.getBean(ShopInfo.class).name()).isEqualTo("Z-ARGUMENTU");
            }
        } finally {
            System.clearProperty("cfg01.shop.name");
        }
        // (d) tej klasy: plik mówi 23, JVM (ustawiona w @BeforeAll) mówi 7, properties w adnotacji mówi 19 → wygrywa 19
        assertThat(shop.vatPercent()).isEqualTo(19);
        // (e) max-items: plik mówi 10, a properties w adnotacji 99 → 99
        assertThat(shop.maxItems()).isEqualTo(99);
    }

    @Test
    @DisplayName("7. PUŁAPKA: @Value na polu jest null w konstruktorze, a na polu statycznym nie działa wcale (log tylko INFO)")
    @ExtendWith(OutputCaptureExtension.class)   // przechwytuje to, co aplikacja wypisała (także log)
    void valueOnFieldPitfalls(CapturedOutput output) {
        new ApplicationContextRunner()
                .withUserConfiguration(Cfg01Properties.FieldInjectionDemo.class, Cfg01Properties.StaticFieldDemo.class)
                .withPropertyValues("cfg01.shop.name=Kawiarnia")
                .run(context -> {
                    Cfg01Properties.FieldInjectionDemo demo = context.getBean(Cfg01Properties.FieldInjectionDemo.class);
                    assertThat(demo.nameSeenInConstructor).isNull();   // w konstruktorze: jeszcze null
                    assertThat(demo.name).isEqualTo("Kawiarnia");      // po utworzeniu beana: jest
                    assertThat(Cfg01Properties.StaticFieldDemo.name).isNull();   // statyczne: nigdy
                });
        // jedyny ślad w logu: wpis na poziomie INFO (nie WARN)
        assertThat(output.getAll().lines().filter(line -> line.contains("not supported on static fields")))
                .isNotEmpty()
                .allMatch(line -> line.contains("INFO") && !line.contains("WARN"));
    }

    @Test
    @DisplayName("2+8. @Value bez ${ } wstrzykuje dosłowny tekst, a brak klucza bez wartości domyślnej zatrzymuje start")
    void literalAndMissingKey() {
        new ApplicationContextRunner().withUserConfiguration(NoBraces.class)
                .withPropertyValues("cfg01.shop.name=Kawiarnia")
                .run(context -> assertThat(context.getBean(NoBraces.class).text).isEqualTo("cfg01.shop.name"));

        // Uwaga: sprawdzamy to w aplikacji Boota — w gołym Springu brakujący placeholder jest po cichu zostawiany jak jest
        assertThatThrownBy(() -> bootRun(MissingKey.class))
                .hasStackTraceContaining("Could not resolve placeholder 'cfg01.brak.takiego.klucza'");
    }

    // ---- pomocnicze klasy (bez @Component — rejestrowane ręcznie w testach, więc lekcja ich nie skanuje) ----

    static class EmptyDefault {
        final String x;

        EmptyDefault(@Value("${cfg01.brak:}") String x) {
            this.x = x;
        }
    }

    static class NeedsDuration {
        NeedsDuration(@Value("${cfg01.demo.time}") Duration time) {
        }
    }

    @EnableAutoConfiguration   // autokonfiguracja Boota włącza ścisłe rozwiązywanie ${...}
    static class CamelCasePlaceholder {
        CamelCasePlaceholder(@Value("${cfg01.demo.maxItems}") int maxItems) {
        }
    }

    static class NoBraces {
        final String text;

        NoBraces(@Value("cfg01.shop.name") String text) {
            this.text = text;
        }
    }

    @EnableAutoConfiguration
    static class MissingKey {
        MissingKey(@Value("${cfg01.brak.takiego.klucza}") String value) {
        }
    }

    /** Uruchamia w Bootcie (bez serwera WWW) wskazaną klasę jako źródło beanów i zamyka kontekst, jeśli wystartuje. */
    private static void bootRun(Class<?> source, String... args) {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(source)
                .web(WebApplicationType.NONE).run(args)) {
            // jeśli doszliśmy tutaj, kontekst wystartował
        }
    }

    /** Uruchamia lekcję „jak z wiersza poleceń”: profil cfg01 + podane argumenty. */
    private static ConfigurableApplicationContext start(String... args) {
        return new SpringApplicationBuilder(Cfg01Application.class)
                .profiles("cfg01")
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
