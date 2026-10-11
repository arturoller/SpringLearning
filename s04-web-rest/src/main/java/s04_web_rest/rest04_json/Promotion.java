package s04_web_rest.rest04_json;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * Promotion = promocja z datą w POLSKIM formacie. {@code @JsonFormat(pattern = "dd.MM.yyyy")} (format JSON) zmienia
 * format daty dla tego jednego pola — w obie strony: zapis "31.10.2026" i odczyt "31.10.2026".
 */
public record Promotion(String code, @JsonFormat(pattern = "dd.MM.yyyy") LocalDate validUntil) {
}
