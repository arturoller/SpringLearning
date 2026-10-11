package s04_web_rest.solutions.rest04_json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Rozwiązanie wzorcowe ćwiczenia 1 lekcji Rest04Json — rekord CustomerJson z adnotacjami. */
public record SolutionCustomerJson(
        @JsonProperty("full_name") String fullName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String email,
        @JsonIgnore String password) {
}
