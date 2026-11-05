package s11_testing.test01_test_slices;

import java.math.BigDecimal;
import java.time.Instant;

/** DTO wyjściowe: dokładnie to, co widzi klient. */
public record ProductResponse(long id, String name, BigDecimal price, Instant createdAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getCreatedAt());
    }
}
