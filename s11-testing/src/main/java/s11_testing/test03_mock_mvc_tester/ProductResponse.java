package s11_testing.test03_mock_mvc_tester;

import java.math.BigDecimal;

/**
 * ProductResponse = produkt w odpowiedzi API (JSON). Cena w złotych jako {@code BigDecimal} — w JSON-ie zwykła
 * liczba, np. {@code "price": 64.99}.
 */
public record ProductResponse(String sku, String name, String category, BigDecimal price) {
}
