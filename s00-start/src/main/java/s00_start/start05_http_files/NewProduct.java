package s00_start.start05_http_files;

/**
 * NewProduct = nowy produkt: dane przysyłane w ciele (body) zapytania POST. Bez id — id nadaje serwer.
 * JSON: {"name":"Herbata","priceGrosze":1599}.
 */
public record NewProduct(String name, int priceGrosze) {
}
