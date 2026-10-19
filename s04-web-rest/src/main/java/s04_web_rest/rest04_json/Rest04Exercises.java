package s04_web_rest.rest04_json;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ćwiczenia lekcji Rest04Json — rekordy do opisania adnotacjami i metody do uzupełnienia.
 * Testy: Rest04JsonExercisesTest (uruchamiasz sam: ▶ obok klasy testu). Test używa JsonMappera Boota ({@code @JsonTest}).
 */
public final class Rest04Exercises {

    private Rest04Exercises() {
    }

    /**
     * ĆWICZENIE 1 (łatwe): pozycja zamówienia ma dawać JSON {"sku":"KSI-001","qty":2,"unitPrice":79.00} —
     * pole quantity ma się nazywać "qty", a purchasePrice (cena zakupu) NIE może trafić do JSON-a.
     * Dopisz adnotacje przy składowych rekordu (importy z com.fasterxml.jackson.annotation).
     */
    public record OrderLine(String sku, int quantity, BigDecimal unitPrice, BigDecimal purchasePrice) {
        // TODO: twoje rozwiązanie — adnotacje przy składowych w nawiasie powyżej
    }

    /**
     * ĆWICZENIE 2 (średnie): dostawa ma dawać {"orderId":"ZAM-001","deliveryDate":"21.10.2026"} — data w polskim
     * formacie, a pole courierNote (uwaga dla kuriera) ma ZNIKAĆ, gdy jest null. Gdy nie jest null — ma być w JSON-ie.
     * Dopisz adnotacje (jedna przy dacie, jedna na rekordzie albo przy polu courierNote).
     */
    public record Delivery(String orderId, LocalDate deliveryDate, String courierNote) {
        // TODO: twoje rozwiązanie — adnotacje
    }

    /** Metka cenowa z JSON-a (ćwiczenie 3): sku, cena jednostkowa i liczba sztuk. */
    public record PriceTag(String sku, BigDecimal price, int quantity) {
    }

    /**
     * ĆWICZENIE 3 (średnie): json to TABLICA metek, np.
     * [{"sku":"KSI-001","price":79.00,"quantity":2},{"sku":"SPO-002","price":7.49,"quantity":4}].
     * Odczytaj ją mapperem do {@code List<PriceTag>} i zwróć wartość całości: suma price × quantity (tu 187.96).
     * Podpowiedź: {@code mapper.readValue(json, new TypeReference<List<PriceTag>>() {})} — TypeReference z pakietu
     * tools.jackson.core.type. Wyjątki Jacksona 3 są niekontrolowane — bez throws.
     */
    public static BigDecimal totalValue(JsonMapper mapper, String json) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): customizer „ścisłego sklepu” — mapper po dostrojeniu ma (a) POMIJAĆ pola null w każdej
     * klasie i (b) zgłaszać błąd przy nieznanym polu w JSON-ie. Zwróć lambdę builder -> ...
     * Podpowiedź:
     * {@code builder.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))}
     * oraz {@code builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)} (tools.jackson.databind).
     * W aplikacji taki customizer byłby beanem ({@code @Bean}) w klasie {@code @Configuration} — jak w JsonConfig.
     */
    public static JsonMapperBuilderCustomizer strictShopCustomizer() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
