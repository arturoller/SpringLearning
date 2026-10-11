package s04_web_rest.rest03_responses;

/** Product = produkt sklepu (rekord, niezmienny). Cena w groszach. */
public record Product(long id, String name, int priceGrosze) {
}
