package s22_extras.ext04_tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import s22_extras.ext04_tracing.AvailabilityService.Availability;
import s22_extras.ext04_tracing.StockController.StockLevel;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Testy lekcji Ext04Tracing. Prawdziwy serwer (RANDOM_PORT), bo ślad ma przejść przez sieć: test → sklep → (HTTP)
 * → magazyn. {@code @AutoConfigureTracing} WŁĄCZA śledzenie w teście (Boot 4 domyślnie je w testach wyłącza — patrz
 * Ext04TracingDisabledInTestsTest). Ślady trafiają do eksportera W PAMIĘCI — żadnego zewnętrznego serwera.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.tracing.sampling.probability=1.0")   // każdy ślad (domyślnie 10%)
@AutoConfigureTracing
@Import(Ext04TracingTest.InMemoryTracing.class)
@ExtendWith(OutputCaptureExtension.class)   // przechwytuje to, co aplikacja wypisała (logi)
class Ext04TracingTest {

    /** Eksporter w pamięci jako bean: Boot dołącza każdy bean SpanExporter do procesora odcinków. */
    @TestConfiguration(proxyBeanMethods = false)
    static class InMemoryTracing {
        @Bean
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }

    static final String ROOT = "http get /api/shop/{sku}/availability";
    static final String STOCK_SERVER = "http get /api/stock/{sku}";

    @LocalServerPort
    int port;

    @Autowired
    InMemorySpanExporter exporter;

    @Autowired
    SdkTracerProvider tracerProvider;

    @Autowired
    Tracer tracer;

    @Autowired
    RestClient.Builder restClientBuilder;

    @BeforeEach
    void clearSpans() {
        exporter.reset();
    }

    /** Zapytanie z „przeglądarki” (zwykły RestClient testu — bez śledzenia, więc sklep zaczyna NOWY ślad). */
    Availability askShop(String sku) {
        return RestClient.create("http://localhost:" + port).get()
                .uri("/api/shop/{sku}/availability", sku).retrieve().body(Availability.class);
    }

    /**
     * Odcinki trafiają do eksportera asynchronicznie (BatchSpanProcessor, co 5 s), a odcinek serwera kończy się chwilę
     * PO wysłaniu odpowiedzi. Dlatego: wymuś wysłanie (forceFlush) i czekaj, aż pojawi się odcinek główny.
     */
    List<SpanData> finishedSpansWithRoot() {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            tracerProvider.forceFlush().join(5, TimeUnit.SECONDS);
            assertThat(exporter.getFinishedSpanItems()).extracting(SpanData::getName).contains(ROOT, STOCK_SERVER);
        });
        return exporter.getFinishedSpanItems();
    }

    static SpanData span(List<SpanData> spans, String name) {
        return spans.stream().filter(s -> s.getName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("2. Jeden ślad przez dwie „usługi”: wszystkie odcinki mają ten sam traceId, korzeń to SERVER sklepu")
    void oneTraceAcrossServices() {
        Availability availability = askShop("ELE-001");

        List<SpanData> spans = finishedSpansWithRoot();
        assertThat(spans).extracting(SpanData::getTraceId).containsOnly(availability.traceId());
        SpanData root = span(spans, ROOT);
        assertThat(root.getKind()).isEqualTo(SpanKind.SERVER);
        assertThat(root.getParentSpanId()).isEqualTo("0000000000000000");   // brak rodzica = korzeń śladu
        assertThat(availability.stock()).isEqualTo(7);
    }

    @Test
    @DisplayName("3. Propagacja: magazyn dostał traceparent 00-<traceId>-<odcinek CLIENT>-03 i jest jego dzieckiem")
    void traceparentPropagation() {
        Availability availability = askShop("ELE-001");

        List<SpanData> spans = finishedSpansWithRoot();
        SpanData client = span(spans, "http get");
        SpanData stockServer = span(spans, STOCK_SERVER);
        assertThat(client.getKind()).isEqualTo(SpanKind.CLIENT);
        assertThat(availability.stockSawTraceparent())
                .isEqualTo("00-" + availability.traceId() + "-" + client.getSpanId() + "-03");
        assertThat(stockServer.getParentSpanId()).isEqualTo(client.getSpanId());
    }

    @Test
    @DisplayName("4. Własna obserwacja = odcinek sprawdz-dostepnosc z atrybutami; jego dzieckiem jest wywołanie HTTP")
    void customObservationSpan() {
        askShop("SPO-001");

        List<SpanData> spans = finishedSpansWithRoot();
        SpanData custom = span(spans, "sprawdz-dostepnosc");
        assertThat(custom.getKind()).isEqualTo(SpanKind.INTERNAL);
        assertThat(custom.getAttributes().asMap()).extractingFromEntries(e -> e.getKey().getKey() + "=" + e.getValue())
                .contains("sklep.kategoria=SPO", "sklep.sku=SPO-001");
        assertThat(span(spans, "http get").getParentSpanId()).isEqualTo(custom.getSpanId());
    }

    @Test
    @DisplayName("2. PUŁAPKA (szum): Spring Security dokłada własne odcinki do każdego zapytania")
    void securitySpans() {
        askShop("ELE-001");

        assertThat(finishedSpansWithRoot()).extracting(SpanData::getName)
                .contains("security filterchain before", "authorize request", "secured request", "security filterchain after");
    }

    @Test
    @DisplayName("5. Logi obu „usług” mają [traceId-spanId] z TYM SAMYM traceId")
    void traceIdInLogs(CapturedOutput output) {
        Availability availability = askShop("ELE-001");

        List<String> lines = output.getAll().lines()
                .filter(line -> line.contains("Sklep: sprawdzam dostępność ELE-001") || line.contains("Magazyn: sprawdzam stan ELE-001"))
                .toList();
        assertThat(lines).hasSize(2).allSatisfy(line -> assertThat(line).contains("[" + availability.traceId() + "-"));
    }

    @Test
    @DisplayName("3. PUŁAPKA: RestClient.create() nie ma śledzenia — magazyn nie dostaje traceparent; builder z Boota — dostaje")
    void plainRestClientBreaksTrace() {
        String stockUrl = "http://localhost:" + port + "/api/stock/ELE-001";
        Span span = tracer.nextSpan().name("test-wywolanie").start();
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {   // „jesteśmy w trakcie śladu”
            StockLevel plain = RestClient.create().get().uri(stockUrl).retrieve().body(StockLevel.class);
            StockLevel fromBoot = restClientBuilder.build().get().uri(stockUrl).retrieve().body(StockLevel.class);

            assertThat(plain.receivedTraceparent()).isNull();
            assertThat(fromBoot.receivedTraceparent()).startsWith("00-" + span.context().traceId() + "-");
        } finally {
            span.end();
        }
    }
}
