package s04_web_rest.rest04_json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Klient sklepu — rekord z adnotacjami Jacksona (pakiet {@code com.fasterxml.jackson.annotation} — to się NIE zmieniło
 * w Jacksonie 3, zmieniły się tylko pakiety klas jak JsonMapper).
 * <ul>
 *   <li>{@code fullName} w JSON nazywa się {@code full_name},</li>
 *   <li>{@code passwordHash} nigdy nie opuszcza serwera (ani nie jest czytany z żądania),</li>
 *   <li>{@code phone} znika z JSON, gdy jest null,</li>
 *   <li>{@code birthDate} ma własny format {@code dd.MM.yyyy}.</li>
 * </ul>
 */
public record Customer(
        long id,
        @JsonProperty("full_name") String fullName,
        @JsonIgnore String passwordHash,
        @JsonInclude(JsonInclude.Include.NON_NULL) String phone,
        @JsonFormat(pattern = "dd.MM.yyyy") LocalDate birthDate) {
}
