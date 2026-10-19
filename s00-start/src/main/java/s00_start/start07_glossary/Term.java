package s00_start.start07_glossary;

/**
 * Jedno hasło słowniczka (record = rekord: niezmienna klasa danych, którą Java sama wyposaża w konstruktor,
 * akcesory, equals, hashCode i toString).
 *
 * @param english    angielska nazwa pojęcia, tak jak zobaczysz ją w dokumentacji i w kodzie (np. {@code bean})
 * @param polish     polski odpowiednik używany w kursie
 * @param definition definicja w 1-2 zdaniach, prostym językiem
 * @param lesson     odesłanie do lekcji: {@code sNN_dzial/Klasa} (ten kurs), {@code sNN_dzial} (dział bez nazw lekcji)
 *                   albo {@code tNN_pakiet/Klasa} (JavaLearning)
 */
public record Term(String english, String polish, String definition, String lesson) {
}
