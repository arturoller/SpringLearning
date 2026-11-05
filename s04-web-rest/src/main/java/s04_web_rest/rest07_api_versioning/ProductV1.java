package s04_web_rest.rest07_api_versioning;

/** Kształt produktu w wersji 1.0 API: cena jako liczba groszy. */
public record ProductV1(long id, String name, long priceGrosze) {
}
