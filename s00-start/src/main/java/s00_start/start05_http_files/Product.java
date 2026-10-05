package s00_start.start05_http_files;

/**
 * Product = produkt w sklepie. Rekord (niezmienny) — Jackson zamienia go na JSON {"id":1,"name":"Kawa","priceGrosze":2999}
 * i z powrotem.
 */
public record Product(long id, String name, int priceGrosze) {
}
