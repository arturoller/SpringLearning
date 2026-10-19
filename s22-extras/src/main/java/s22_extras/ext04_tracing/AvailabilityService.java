package s22_extras.ext04_tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import s22_extras.ext04_tracing.StockController.StockLevel;

/**
 * „Usługa sklepu” (lekcja Ext04Tracing, sekcje 2–4): sprawdza dostępność, pytając magazyn przez HTTP.
 *
 * <p>RestClient jest zbudowany z {@code RestClient.Builder} z Boota — dzięki temu ma podpiętą obserwację: każde
 * wywołanie to odcinek (span) typu CLIENT, a do zapytania trafia nagłówek {@code traceparent}.</p>
 */
@Service
public class AvailabilityService {

    /** Wynik: dostępność + identyfikator śladu (do pokazania w odpowiedzi i w teście). */
    public record Availability(String sku, boolean available, int stock, String traceId, String stockSawTraceparent) {
    }

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);

    private final RestClient restClient;
    private final ObservationRegistry observationRegistry;
    private final Environment environment;

    public AvailabilityService(RestClient.Builder restClientBuilder, ObservationRegistry observationRegistry,
                               Environment environment) {
        this.restClient = restClientBuilder.build();        // builder z Boota = obserwacja + propagacja w komplecie
        this.observationRegistry = observationRegistry;
        this.environment = environment;
    }

    public Availability check(String sku, String traceId) {
        log.info("Sklep: sprawdzam dostępność {}", sku);
        // Własny odcinek w śladzie: Observation API (s15_actuator_logging/Obs05Observations) — z tracingiem na
        // classpath każda obserwacja staje się odcinkiem (span) o nazwie z contextualName.
        StockLevel level = Observation.createNotStarted("sklep.dostepnosc", observationRegistry)
                .contextualName("sprawdz-dostepnosc")
                .lowCardinalityKeyValue("sklep.kategoria", sku.substring(0, 3))   // mało różnych wartości → tag
                .highCardinalityKeyValue("sklep.sku", sku)                          // dużo różnych wartości → tylko w śladzie
                .observe(() -> restClient.get()
                        .uri(stockServiceUrl() + "/api/stock/{sku}", sku)
                        .retrieve()
                        .body(StockLevel.class));
        return new Availability(sku, level.stock() > 0, level.stock(), traceId, level.receivedTraceparent());
    }

    /**
     * Adres „magazynu”. W prawdziwym systemie: właściwość z adresem INNEJ usługi. Tu: ta sama aplikacja — port czytamy
     * w chwili wywołania (local.server.port znany dopiero po starcie serwera, także przy losowym porcie w testach).
     */
    private String stockServiceUrl() {
        return "http://localhost:" + environment.getRequiredProperty("local.server.port");
    }
}
