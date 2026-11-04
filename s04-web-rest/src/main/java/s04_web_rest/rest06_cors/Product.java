package s04_web_rest.rest06_cors;

/** Produkt w sklepie (rekord — niezmienny nośnik danych; cena w groszach, żeby uniknąć ułamków). */
public record Product(long id, String name, long priceGrosze) {
}
