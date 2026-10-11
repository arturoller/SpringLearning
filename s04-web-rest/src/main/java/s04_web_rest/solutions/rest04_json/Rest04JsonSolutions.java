package s04_web_rest.solutions.rest04_json;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest04Json — zajrzyj dopiero po własnej próbie! */
public final class Rest04JsonSolutions {

    private Rest04JsonSolutions() {
    }

    /** Pozycja tablicy z ćwiczenia 2 — tylko pola, których potrzebujemy. */
    record Item(String name, int priceGrosze) {
    }

    static String solution1(JsonMapper mapper, SolutionCustomerJson customer) {
        return mapper.writeValueAsString(customer);   // bez try/catch: JacksonException jest niesprawdzany
    }

    static int solution2(JsonMapper mapper, String jsonArray) {
        // TypeReference (odwołanie do typu) — przekazuje pełny typ List<Item>, którego nie da się zapisać jako Item.class
        List<Item> items = mapper.readValue(jsonArray, new TypeReference<List<Item>>() {
        });
        return items.stream().mapToInt(Item::priceGrosze).sum();
    }

    static JsonMapper solution3() {
        return JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }
}
