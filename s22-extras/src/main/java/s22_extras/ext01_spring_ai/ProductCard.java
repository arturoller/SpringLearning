package s22_extras.ext01_spring_ai;

import java.util.List;

/**
 * Karta produktu wygenerowana przez model — odpowiedź jako REKORD (structured output = odpowiedź ustrukturyzowana).
 * Spring AI tworzy z tego rekordu schemat JSON, dokleja go do promptu, a odpowiedź modelu zamienia z powrotem na rekord.
 *
 * @param headline    nagłówek (jedno krótkie zdanie)
 * @param description opis (2–3 zdania)
 * @param tags        słowa kluczowe do wyszukiwarki sklepu
 */
public record ProductCard(String headline, String description, List<String> tags) {
}
