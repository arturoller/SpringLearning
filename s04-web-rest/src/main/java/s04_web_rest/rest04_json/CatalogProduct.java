package s04_web_rest.rest04_json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;

/**
 * CatalogProduct = produkt w katalogu — rekord z adnotacjami Jacksona (sekcje 2–4 lekcji Rest04Json).
 * Adnotacje są w pakiecie com.fasterxml.jackson.annotation także w Jacksonie 3 (artefakt jackson-annotations 2.x
 * jest wspólny dla Jacksona 2 i 3), choć cała reszta Jacksona 3 to pakiety tools.jackson.*.
 *
 * @param id          identyfikator
 * @param name        nazwa — w JSON pod kluczem "product_name" ({@code @JsonProperty} = właściwość JSON)
 * @param priceGrosze cena w groszach
 * @param description opis — pomijany w JSON, gdy null ({@code @JsonInclude(NON_NULL)} = dołącz, gdy nie null)
 * @param addedOn     data dodania (LocalDate) — w JSON jako tekst ISO-8601 "2026-10-01"
 * @param updatedAt   chwila ostatniej zmiany (Instant) — w JSON jako tekst ISO-8601 "2026-10-01T08:30:00Z"
 * @param purchasePriceGrosze cena zakupu — TAJEMNICA sklepu: {@code @JsonIgnore} (ignoruj) nie wypuszcza jej do JSON-a
 */
public record CatalogProduct(
        long id,
        @JsonProperty("product_name") String name,
        int priceGrosze,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        LocalDate addedOn,
        Instant updatedAt,
        @JsonIgnore int purchasePriceGrosze) {
}
