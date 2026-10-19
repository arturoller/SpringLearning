package s11_testing.test02_mockito_beans;

/**
 * Product = produkt sklepu. Rekord (niezmienny). Cena w GROSZACH ({@code long}), bo potrzebujemy tylko prostego
 * mnożenia (cena × ilość) — bez kłopotów z zaokrąglaniem.
 */
public record Product(String sku, String name, long priceGrosze) {
}
