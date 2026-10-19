package s09_jpa_relations.rel03_many_to_many;

/**
 * ProductTagCount = liczba tagów produktu. Rekord (record) jako wynik zapytania JPQL z wyrażeniem konstruktora
 * {@code select new ...ProductTagCount(p.name, count(t))} — ćwiczenie 4 lekcji. count(...) w JPQL zwraca long.
 */
public record ProductTagCount(String productName, long tagCount) {
}
