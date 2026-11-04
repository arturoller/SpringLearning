package s04_web_rest.rest03_responses;

/** Produkt sklepu; cena w groszach. */
public record Product(long id, String name, long priceGrosze) {
}
