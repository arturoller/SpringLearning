package s15_actuator_logging.obs04_logging;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Obs04Logging. Dobór narzędzia: {@code @SpringBootTest} + MockMvc (żeby przeszło zapytanie przez filtr
 * i kontroler) + {@code OutputCaptureExtension} (przechwytuje to, co aplikacja wypisała na konsolę). Zwykłe
 * asercje na tekście logów robimy na stałych, ASCII-owych fragmentach komunikatów (np. "(sztuk: 1)"), a każda klasa
 * zaczyna i kończy się {@link LoggingTestSupport#reset()} — Boot konfiguruje logowanie raz na JVM, więc bez tego
 * wynik zależałby od kolejności testów.
 */
class Obs04LoggingTest {

    private static final String BODY =
            "{\"sku\":\"ELE-001\",\"quantity\":1,\"priceGrosze\":549999,\"email\":\"jan@example.com\"}";
    private static final String BODY_BAD_PRICE =
            "{\"sku\":\"ELE-001\",\"quantity\":1,\"priceGrosze\":0,\"email\":\"jan@example.com\"}";
    private static final String PACKAGE = "s15_actuator_logging.obs04_logging";

    private static void checkout(MockMvc mvc, String requestId, String body) throws Exception {
        mvc.perform(post("/api/checkout").header(RequestIdFilter.HEADER, requestId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static List<String> linesWith(CapturedOutput output, String fragment) {
        return output.getAll().lines().filter(line -> line.contains(fragment)).toList();
    }

    // =================================================================================================
    // 1. Poziomy: domyślnie INFO — DEBUG jest ukryty
    // =================================================================================================

    @Nested   // nested = zagnieżdżona (własny kontekst z własnymi ustawieniami logowania)
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest
    @AutoConfigureMockMvc
    @DisplayName("1. Domyślny poziom INFO: widać INFO, nie widać DEBUG")
    class DefaultLevel {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("komunikat INFO jest wypisany, DEBUG z tego samego wywołania — nie")
        void infoYesDebugNo(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-LEVEL", BODY);

            assertThat(linesWith(output, "klient j***@example.com")).hasSize(1);
            assertThat(linesWith(output, "(sztuk: 1)")).isEmpty();
        }
    }

    // =================================================================================================
    // 2. logging.level.<pakiet> w ustawieniach
    // =================================================================================================

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = "logging.level." + PACKAGE + "=DEBUG")
    @AutoConfigureMockMvc
    @DisplayName("2. logging.level.<pakiet>=DEBUG włącza szczegóły tylko dla tego pakietu")
    class LevelFromProperty {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("widać DEBUG z CheckoutService i PaymentService")
        void debugVisible(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-DEBUG", BODY);

            assertThat(linesWith(output, "(sztuk: 1)")).hasSize(1);   // DEBUG z CheckoutService
            assertThat(linesWith(output, "DEBUG")).anyMatch(line -> line.contains("PaymentService"));
        }
    }

    // =================================================================================================
    // 3. Grupa logów
    // =================================================================================================

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = {
            "logging.group.sklep=" + PACKAGE + ".CheckoutService," + PACKAGE + ".PaymentService",
            "logging.level.sklep=DEBUG"})
    @AutoConfigureMockMvc
    @DisplayName("3. logging.group: jedna nazwa dla kilku rejestratorów, poziom ustawiany raz")
    class LogGroup {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("logging.level.sklep=DEBUG włącza DEBUG w obu klasach grupy")
        void groupSetsBothLoggers(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-GROUP", BODY);

            assertThat(linesWith(output, "DEBUG")).anyMatch(line -> line.contains("CheckoutService"));
            assertThat(linesWith(output, "DEBUG")).anyMatch(line -> line.contains("PaymentService"));
        }
    }

    // =================================================================================================
    // 4. Zmiana poziomu w czasie działania: /actuator/loggers
    // =================================================================================================

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = "management.endpoints.web.exposure.include=loggers")
    @AutoConfigureMockMvc
    @DisplayName("4. /actuator/loggers zmienia poziom bez restartu aplikacji")
    class RuntimeLevelChange {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @AfterEach
        void resetLevel() throws Exception {
            setLevel("null");   // przywracamy dziedziczony poziom
        }

        private void setLevel(String json) throws Exception {
            mvc.perform(post("/actuator/loggers/" + PACKAGE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"configuredLevel\":" + (json.equals("null") ? "null" : "\"" + json + "\"") + "}"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST configuredLevel=DEBUG włącza szczegóły od następnego zapytania; null przywraca INFO")
        void changeLevelAtRuntime(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-BEFORE", BODY);
            assertThat(linesWith(output, "(sztuk: 1)")).isEmpty();

            setLevel("DEBUG");
            mvc.perform(get("/actuator/loggers/" + PACKAGE))
                    .andExpect(jsonPath("$.configuredLevel").value("DEBUG"))
                    .andExpect(jsonPath("$.effectiveLevel").value("DEBUG"));
            checkout(mvc, "REQ-AFTER", BODY);
            assertThat(linesWith(output, "(sztuk: 1)")).hasSize(1);

            setLevel("null");
            mvc.perform(get("/actuator/loggers/" + PACKAGE))
                    .andExpect(jsonPath("$.configuredLevel").doesNotExist())
                    .andExpect(jsonPath("$.effectiveLevel").value("INFO"));
        }
    }

    // =================================================================================================
    // 5. MDC i własny wzorzec
    // =================================================================================================

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = "logging.pattern.console=%5p [%X{requestId:-brak}] %m%n")
    @AutoConfigureMockMvc
    @DisplayName("5. MDC: identyfikator zapytania w każdej linii logu zapytania")
    class MdcAndPattern {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("linia z zapytania ma [REQ-MDC] z nagłówka X-Request-Id, a MDC jest po zapytaniu puste")
        void requestIdInLogLine(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-MDC", BODY);

            assertThat(linesWith(output, "klient j***@example.com"))
                    .singleElement().asString().contains("INFO [REQ-MDC]");
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();   // filtr posprzątał w finally
        }

        @Test
        @DisplayName("log spoza zapytania dostaje wartość domyślną [brak] (składnia %X{klucz:-wartość})")
        void defaultOutsideRequest(CapturedOutput output) {
            LoggerFactory.getLogger("test.obs04.poza").info("log poza zapytaniem HTTP");

            assertThat(linesWith(output, "log poza zapytaniem HTTP")).singleElement().asString().contains("[brak]");
        }

        @Test
        @DisplayName("bez nagłówka filtr generuje identyfikator i zwraca go w odpowiedzi")
        void generatedId() throws Exception {
            mvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(header().exists(RequestIdFilter.HEADER));
        }
    }

    // =================================================================================================
    // 6. Logowanie strukturalne (JSON)
    // =================================================================================================

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = "logging.structured.format.console=ecs")
    @AutoConfigureMockMvc
    @DisplayName("6. logging.structured.format.console=ecs — każda linia to JSON (Elastic Common Schema)")
    class StructuredEcs {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("linia INFO jest JSON-em z polami log.level, message i requestId (z MDC)")
        void ecsJson(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-ECS", BODY);

            String line = linesWith(output, "klient j***@example.com").getFirst();
            JsonNode json = JsonMapper.builder().build().readTree(line);
            assertThat(json.path("log").path("level").asString()).isEqualTo("INFO");
            assertThat(json.path("message").asString()).contains("klient j***@example.com");
            assertThat(json.path("requestId").asString()).isEqualTo("REQ-ECS");
            assertThat(json.has("@timestamp")).isTrue();
        }
    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = "logging.structured.format.console=logstash")
    @AutoConfigureMockMvc
    @DisplayName("6. logging.structured.format.console=logstash — JSON w formacie Logstash")
    class StructuredLogstash {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("linia INFO ma pola level, message i requestId — nazwy innego formatu niż w ECS")
        void logstashJson(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-LS", BODY);

            String line = linesWith(output, "klient j***@example.com").getFirst();
            JsonNode json = JsonMapper.builder().build().readTree(line);
            assertThat(json.path("level").asString()).isEqualTo("INFO");
            assertThat(json.path("message").asString()).contains("klient j***@example.com");
            assertThat(json.path("requestId").asString()).isEqualTo("REQ-LS");
            assertThat(json.has("@version")).isTrue();
        }
    }

    // =================================================================================================
    // 7. Zapis do pliku
    // =================================================================================================

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest(properties = "logging.file.name=target/obs04-test.log")
    @AutoConfigureMockMvc
    @DisplayName("7. logging.file.name — logi także do pliku")
    class LogToFile {

        private static final Path FILE = Path.of("target", "obs04-test.log");

        @BeforeAll
        static void freshLogging() throws Exception {
            Files.deleteIfExists(FILE);   // zaczynamy od czystego pliku
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("komunikat trafia do pliku target/obs04-test.log, a w pliku nie ma kodów kolorów ANSI")
        void messageInFile() throws Exception {
            checkout(mvc, "REQ-FILE", BODY);

            String content = Files.readString(FILE);
            assertThat(content).contains("klient j***@example.com");
            assertThat(content).doesNotContain("\u001B[");
        }
    }

    // =================================================================================================
    // 8. Wyjątek w logu i bezpieczeństwo danych
    // =================================================================================================

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @SpringBootTest
    @AutoConfigureMockMvc
    @DisplayName("8. Wyjątek jako ostatni argument daje ślad stosu; dane osobowe są maskowane")
    class ExceptionsAndPrivacy {

        @BeforeAll
        static void freshLogging() {
            LoggingTestSupport.reset();
        }

        @AfterAll
        static void restoreLogging() {
            LoggingTestSupport.reset();
        }

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("log.error(komunikat, wyjątek) wypisuje ślad stosu z PaymentService.charge")
        void stackTraceIsPrinted(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-ERR", BODY_BAD_PRICE);

            assertThat(output.getAll()).contains("java.lang.IllegalStateException: Kwota p");
            assertThat(output.getAll()).contains("at " + PACKAGE + ".PaymentService.charge");
        }

        @Test
        @DisplayName("PUŁAPKA: sklejony wyjątek albo getMessage() gubi ślad stosu; {} z wyjątkiem zostaje puste, ślad jest")
        void exceptionVariants(CapturedOutput output) {
            Logger log = LoggerFactory.getLogger("test.obs04.wyjatek");
            IllegalStateException e = new IllegalStateException("zepsute-w-placeholderze");

            log.error("A: " + e);                // ŹLE: tylko tekst wyjątku, bez śladu stosu
            log.error("B: {}", e.getMessage());  // ŹLE: tylko komunikat, bez śladu stosu
            log.error("C: {}", e);               // dziwne: {} zostaje dosłownie (wyjątek poszedł do śladu), ślad jest
            log.error("D: komunikat", e);        // DOBRZE: wyjątek jako ostatni argument — ślad stosu jest

            assertThat(linesWith(output, "A: java.lang.IllegalStateException: zepsute-w-placeholderze")).hasSize(1);
            assertThat(linesWith(output, "B: zepsute-w-placeholderze")).hasSize(1);
            assertThat(linesWith(output, "C: {}")).hasSize(1);
            assertThat(linesWith(output, "D: komunikat")).hasSize(1);
            // linie śladu stosu zawierają nazwę tej metody testowej — tylko wywołania C i D wypisały ślad
            assertThat(linesWith(output, "ExceptionsAndPrivacy.exceptionVariants")).hasSize(2);
        }

        @Test
        @DisplayName("e-mail klienta nie trafia do logu jawnie — tylko zamaskowany")
        void emailIsMasked(CapturedOutput output) throws Exception {
            checkout(mvc, "REQ-PII", BODY);

            assertThat(output.getAll()).contains("j***@example.com");
            assertThat(output.getAll()).doesNotContain("jan@example.com");
        }
    }

    // =================================================================================================
    // 9. Bez Springa: parametry {} i maskowanie
    // =================================================================================================

    /** Obiekt zliczający wywołania toString() — pokazuje, KIEDY powstaje tekst komunikatu. */
    static class CountingObject {
        int toStringCalls;

        @Override
        public String toString() {
            toStringCalls++;
            return "dane";
        }
    }

    @Test
    @DisplayName("9. PUŁAPKA: sklejanie napisów buduje tekst nawet przy wyłączonym DEBUG; {} — nie")
    void concatenationBuildsTextEvenWhenDisabled() {
        Logger log = LoggerFactory.getLogger("test.obs04.sklejanie");   // poziom dziedziczony: INFO, DEBUG wyłączony
        assertThat(log.isDebugEnabled()).isFalse();
        CountingObject concatenated = new CountingObject();
        CountingObject parameterized = new CountingObject();

        log.debug("Dane: " + concatenated);        // ŹLE: toString() wywołany ZAWSZE, jeszcze przed logowaniem
        log.debug("Dane: {}", parameterized);      // DOBRZE: logger składa tekst dopiero, gdy poziom jest włączony

        assertThat(concatenated.toStringCalls).isEqualTo(1);
        assertThat(parameterized.toStringCalls).isZero();
    }

    @Test
    @DisplayName("9. maskEmail: pierwsza litera, trzy gwiazdki i domena; brak e-maila → \"brak\"")
    void maskEmail() {
        assertThat(CheckoutService.maskEmail("jan@example.com")).isEqualTo("j***@example.com");
        assertThat(CheckoutService.maskEmail("adam.mazur@example.com")).isEqualTo("a***@example.com");
        assertThat(CheckoutService.maskEmail(null)).isEqualTo("brak");
        assertThat(CheckoutService.maskEmail("  ")).isEqualTo("brak");
        assertThat(CheckoutService.maskEmail("bez-malpy")).isEqualTo("***");
    }
}
