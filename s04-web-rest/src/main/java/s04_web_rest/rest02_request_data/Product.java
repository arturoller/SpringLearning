package s04_web_rest.rest02_request_data;

import java.util.List;

/**
 * Produkt sklepu (record = rekord, niezmienna klasa na dane). Cena w groszach (long) — unikamy liczb zmiennoprzecinkowych
 * przy pieniądzach (t09_records/Records01Basics).
 */
public record Product(long id, String name, String category, long priceGrosze, List<String> tags) {
}
