package s09_jpa_relations.rel05_n_plus_one;

/** OrderLineCount = numer zamówienia i liczba jego pozycji — wynik projekcji DTO z ćwiczenia 2 lekcji. */
public record OrderLineCount(String number, long lineCount) {
}
