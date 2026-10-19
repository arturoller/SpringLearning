package s04_web_rest.rest04_json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Modele lekcji Rest04Json — małe rekordy, każdy pokazuje inną rzecz w JSON-ie.
 * UWAGA na importy: adnotacje Jacksona 3 nadal są w pakiecie com.fasterxml.jackson.annotation (biblioteka
 * jackson-annotations 2.x jest wspólna dla Jacksona 2 i 3), a cała reszta Jacksona 3 — w pakiecie tools.jackson.
 */
public final class JsonModels {

    private JsonModels() {
    }

    /** Kategoria produktu. Bez własnego toString() — dlaczego to ważne w Jacksonie 3, pokazuje sekcja 2 lekcji. */
    public enum Category {
        ELEKTRONIKA, SPOZYWCZE, KSIAZKI, ODZIEZ, DOM
    }

    /**
     * Enum z WŁASNYM toString() (np. dla ładnych napisów w interfejsie). Jackson 3 domyślnie pisze i czyta enumy
     * przez toString() — więc w JSON-ie pojawi się "książki", a nie "KSIAZKI". PUŁAPKA z sekcji 2.
     */
    public enum PrettyCategory {
        ELEKTRONIKA("elektronika"), KSIAZKI("książki");

        private final String label;

        PrettyCategory(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Karta produktu BEZ żadnych adnotacji — domyślny JSON Jacksona 3 w Boot 4:
     * {"sku":"ELE-001","name":"Laptop Pro 14","category":"ELEKTRONIKA","price":5499.99,"stock":7,
     * "addedOn":"2026-10-19","updatedAt":"2026-10-19T08:30:00Z"}.
     */
    public record ProductCard(String sku, String name, Category category, BigDecimal price, int stock,
                              LocalDate addedOn, Instant updatedAt) {
    }

    /**
     * Produkt „na zewnątrz” — z adnotacjami Jacksona:
     * {@code @JsonProperty("productName")} — inna nazwa pola w JSON-ie niż w Javie,
     * {@code @JsonIgnore} — pole w ogóle nie trafia do JSON-a (cena zakupu to tajemnica sklepu),
     * {@code @JsonFormat(pattern = "dd.MM.yyyy")} — data w polskim formacie zamiast ISO,
     * {@code @JsonFormat(shape = STRING)} — liczba wysłana jako tekst "5499.99" (bez ryzyka zaokrągleń po stronie klienta).
     */
    public record PublicProduct(
            String sku,
            @JsonProperty("productName") String name,
            @JsonIgnore BigDecimal purchasePrice,
            @JsonFormat(pattern = "dd.MM.yyyy") LocalDate addedOn,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal price) {
    }

    /** Klient — email może być null (Maria Nowak go nie podała). Domyślnie JSON zawiera "email":null. */
    public record Customer(long id, String name, String email) {
    }

    /** Ten sam klient, ale {@code @JsonInclude(NON_NULL)} — pola null są POMIJANE w JSON-ie (tylko w tej klasie). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CustomerWithoutNulls(long id, String name, String email) {
    }

    /** Dane nowego produktu w ciele POST — tu sprawdzamy, jak Jackson 3 CZYTA JSON (nieznane pola, brak int, enum). */
    public record NewProduct(String sku, String name, Category category, BigDecimal price, int stock) {
    }

    /** Produkt z „ładną” kategorią — do testu pułapki z enum.toString(). */
    public record LabeledProduct(String sku, PrettyCategory category) {
    }
}
