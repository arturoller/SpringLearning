package s19_deploy.dep03_production_config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Testy lekcji Dep03ProductionConfig. Prawdziwych zmiennych środowiska komputera NIE używamy (testy zależałyby od
 * maszyny): podstawiamy sztuczne źródło o PRAWDZIWEJ nazwie "systemEnvironment". Aplikacja startuje bez serwera
 * WWW (WebApplicationType.NONE) — do sprawdzania ustawień wystarczy sam kontekst.
 */
@ExtendWith(OutputCaptureExtension.class)
class Dep03ProductionConfigTest {

    @TempDir
    Path tempDir;   // tymczasowy katalog tworzony i sprzątany przez JUnit

    /**
     * Środowisko z podmienionymi zmiennymi systemu. Źródło MUSI nazywać się "systemEnvironment"
     * (StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) — tylko wtedy Boot stosuje do niego reguły nazw
     * zmiennych środowiska (PUŁAPKA z sekcji 1).
     */
    private static StandardEnvironment osEnvironment(Map<String, Object> variables) {
        StandardEnvironment environment = new StandardEnvironment();
        String name = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        environment.getPropertySources().replace(name, new SystemEnvironmentPropertySource(name, variables));
        return environment;
    }

    private static ConfigurableApplicationContext start(Map<String, Object> osVariables, String... args) {
        return new SpringApplicationBuilder(Dep03Application.class)
                .environment(osEnvironment(osVariables))
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Test
    @DisplayName("1. Zmienna środowiska SHOP_PAYMENT_APIKEY (i starszy zapis SHOP_PAYMENT_API_KEY) wiąże się z shop.payment.api-key")
    void environmentVariablesBindToProperties() {
        try (ConfigurableApplicationContext context = start(
                Map.of("SHOP_PAYMENT_URL", "http://bramka.test", "SHOP_PAYMENT_APIKEY", "klucz-testowy-1"))) {
            PaymentSettings settings = context.getBean(PaymentSettings.class);
            assertThat(settings.apiKey()).isEqualTo("klucz-testowy-1");
            assertThat(settings.url().toString()).isEqualTo("http://bramka.test");
            assertThat(settings.timeout()).isEqualTo(Duration.ofSeconds(5));   // wartość domyślna z @DefaultValue
        }
        try (ConfigurableApplicationContext context = start(
                Map.of("SHOP_PAYMENT_URL", "http://bramka.test", "SHOP_PAYMENT_API_KEY", "klucz-testowy-2"))) {
            assertThat(context.getBean(PaymentSettings.class).apiKey()).isEqualTo("klucz-testowy-2");
        }
    }

    @Test
    @DisplayName("2. Priorytet: plik profilu prod (3s) < zmienna środowiska (7s) < argument (9s)")
    void priorityFileEnvironmentArgument() {
        Map<String, Object> keyOnly = Map.of("SHOP_PAYMENT_APIKEY", "klucz-testowy");

        try (ConfigurableApplicationContext context = start(keyOnly, "--spring.profiles.active=prod")) {
            assertThat(context.getBean(PaymentSettings.class).timeout()).isEqualTo(Duration.ofSeconds(3));
        }
        Map<String, Object> keyAndTimeout = Map.of("SHOP_PAYMENT_APIKEY", "klucz-testowy", "SHOP_PAYMENT_TIMEOUT", "7s");
        try (ConfigurableApplicationContext context = start(keyAndTimeout, "--spring.profiles.active=prod")) {
            assertThat(context.getBean(PaymentSettings.class).timeout()).isEqualTo(Duration.ofSeconds(7));
        }
        try (ConfigurableApplicationContext context = start(keyAndTimeout,
                "--spring.profiles.active=prod", "--shop.payment.timeout=9s")) {
            assertThat(context.getBean(PaymentSettings.class).timeout()).isEqualTo(Duration.ofSeconds(9));
        }
    }

    @Test
    @DisplayName("3. Profil prod: adres bramki i poziom logów z pliku; w pliku NIE ma klucza API ani debug")
    void prodProfileHasNoSecrets() {
        try (ConfigurableApplicationContext context = start(Map.of("SHOP_PAYMENT_APIKEY", "klucz-testowy"),
                "--spring.profiles.active=prod")) {
            assertThat(context.getBean(PaymentSettings.class).url().toString()).isEqualTo("https://platnosci.example.com/api");
            assertThat(context.getEnvironment().getProperty("logging.level.root")).isEqualTo("WARN");
            assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
            assertThat(context.getEnvironment().getProperty("debug")).isNull();
        }
        // Sam profil prod bez klucza z zewnątrz: aplikacja nie ma prawa wystartować — klucza nie ma w repozytorium
        Throwable failure = catchThrowable(() -> start(Map.of(), "--spring.profiles.active=prod").close());
        assertThat(failure).isNotNull();
        assertThat(rootCause(failure)).hasMessageContaining("shop.payment.api-key");
    }

    @Test
    @DisplayName("3. Pliki spoza JAR-a: optional:file (jest), optional:file (brak — start bez błędu), file (brak — błąd), configtree")
    void externalConfigFiles() throws IOException {
        // (a) plik z sekretem poza JAR-em
        Path secrets = tempDir.resolve("secrets.properties");
        Files.writeString(secrets, "shop.payment.api-key=z-pliku-sekretow-123\n");
        try (ConfigurableApplicationContext context = start(Map.of(), "--shop.payment.url=http://bramka.test",
                "--spring.config.import=optional:file:" + secrets)) {
            assertThat(context.getBean(PaymentSettings.class).apiKey()).isEqualTo("z-pliku-sekretow-123");
        }

        // (b) „optional:” i brak pliku — aplikacja startuje (tu klucz podajemy argumentem)
        Path missing = tempDir.resolve("brak.properties");
        try (ConfigurableApplicationContext context = start(Map.of(), "--shop.payment.url=http://bramka.test",
                "--shop.payment.api-key=klucz-z-argumentu", "--spring.config.import=optional:file:" + missing)) {
            assertThat(context.getBean(PaymentSettings.class).apiKey()).isEqualTo("klucz-z-argumentu");
        }

        // (c) bez „optional:” brak pliku przerywa start
        Throwable failure = catchThrowable(() -> start(Map.of(), "--shop.payment.url=http://bramka.test",
                "--shop.payment.api-key=klucz-z-argumentu", "--spring.config.import=file:" + missing).close());
        assertThat(failure).isInstanceOf(ConfigDataResourceNotFoundException.class);
        assertThat(failure.getMessage()).startsWith("Config data resource 'file [").endsWith("cannot be found");

        // (d) katalog sekretów (configtree): plik shop/payment/api-key → klucz shop.payment.api-key
        Path tree = tempDir.resolve("run-secrets");
        Files.createDirectories(tree.resolve("shop/payment"));
        Files.writeString(tree.resolve("shop/payment/api-key"), "z-katalogu-sekretow");
        try (ConfigurableApplicationContext context = start(Map.of(), "--shop.payment.url=http://bramka.test",
                "--spring.config.import=optional:configtree:" + tree + "/")) {
            assertThat(context.getBean(PaymentSettings.class).apiKey()).isEqualTo("z-katalogu-sekretow");
        }
    }

    @Test
    @DisplayName("4. Fail fast: bez klucza API aplikacja nie startuje, a przyczyną jest czytelny komunikat")
    void missingSecretStopsStartup() {
        Throwable failure = catchThrowable(() -> start(Map.of("SHOP_PAYMENT_URL", "http://bramka.test")).close());

        assertThat(failure).isNotNull();
        assertThat(rootCause(failure)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shop.payment.api-key")
                .hasMessageContaining("SHOP_PAYMENT_APIKEY");
    }

    @Test
    @DisplayName("5. Sekret nie wycieka: toString rekordu i maskedApiKey ukrywają klucz")
    void secretsAreMasked() {
        PaymentSettings settings = new PaymentSettings(java.net.URI.create("http://bramka.test"),
                "sekretny-klucz-123", Duration.ofSeconds(5));

        assertThat(settings.maskedApiKey()).isEqualTo("****************23");
        assertThat(settings.toString()).doesNotContain("sekretny").contains("apiKey=****************23");
        // krótki klucz maskujemy w całości
        assertThat(new PaymentSettings(java.net.URI.create("http://x"), "kk", Duration.ofSeconds(1)).maskedApiKey())
                .isEqualTo("**");
    }

    @Test
    @DisplayName("6. --debug włącza raport warunków autokonfiguracji; bez niego raportu nie ma")
    void debugFlagPrintsConditionsReport(CapturedOutput output) {
        Map<String, Object> keyOnly = Map.of("SHOP_PAYMENT_APIKEY", "klucz-testowy", "SHOP_PAYMENT_URL", "http://x");

        start(keyOnly).close();
        assertThat(output.getOut()).doesNotContain("CONDITIONS EVALUATION REPORT");

        start(keyOnly, "--debug").close();
        assertThat(output.getAll()).contains("CONDITIONS EVALUATION REPORT");
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
