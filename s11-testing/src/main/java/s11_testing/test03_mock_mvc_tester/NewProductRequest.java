package s11_testing.test03_mock_mvc_tester;

import java.math.BigDecimal;

/** NewProductRequest = ciało żądania POST /api/products (JSON → rekord). */
public record NewProductRequest(String sku, String name, String category, BigDecimal price) {
}
