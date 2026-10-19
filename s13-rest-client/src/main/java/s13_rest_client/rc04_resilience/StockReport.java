package s13_rest_client.rc04_resilience;

/**
 * StockReport = raport magazynowy dostawcy (report = raport): ile produktów ma w ofercie i ile sztuk łącznie.
 * „Ciężkie” zapytanie — dostawca prosi, żeby jeden sklep wysyłał najwyżej 2 takie zapytania naraz.
 */
public record StockReport(int products, int totalAvailable) {
}
