package s04_web_rest.rest03_responses;

/** NewProduct = dane nowego produktu w ciele POST — bez id (id nadaje serwer). */
public record NewProduct(String name, int priceGrosze) {
}
