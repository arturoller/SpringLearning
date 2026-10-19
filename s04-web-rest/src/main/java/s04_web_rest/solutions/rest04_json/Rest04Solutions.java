package s04_web_rest.solutions.rest04_json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import s04_web_rest.rest04_json.Rest04Exercises.PriceTag;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest04Json — zajrzyj dopiero po własnej próbie! */
public final class Rest04Solutions {

    private Rest04Solutions() {
    }

    /** Rozwiązanie 1: inna nazwa pola (@JsonProperty) i pole ukryte (@JsonIgnore). */
    public record OrderLine(String sku,
                            @JsonProperty("qty") int quantity,
                            BigDecimal unitPrice,
                            @JsonIgnore BigDecimal purchasePrice) {
    }

    /** Rozwiązanie 2: format daty i pomijanie null w tej jednej klasie. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Delivery(String orderId,
                           @JsonFormat(pattern = "dd.MM.yyyy") LocalDate deliveryDate,
                           String courierNote) {
    }

    /** Rozwiązanie 3: TypeReference zachowuje typ elementów listy (bez niego byłyby to mapy, nie PriceTag). */
    public static BigDecimal totalValue(JsonMapper mapper, String json) {
        List<PriceTag> tags = mapper.readValue(json, new TypeReference<List<PriceTag>>() {
        });
        return tags.stream()
                .map(tag -> tag.price().multiply(BigDecimal.valueOf(tag.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Rozwiązanie 4: dwie zmiany na budowniczym — domyślne pomijanie null i ścisłe nieznane pola. */
    public static JsonMapperBuilderCustomizer strictShopCustomizer() {
        return builder -> builder
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
