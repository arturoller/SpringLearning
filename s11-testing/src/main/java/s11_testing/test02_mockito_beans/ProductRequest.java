package s11_testing.test02_mockito_beans;

import java.math.BigDecimal;

/** DTO wejściowe dla POST: nazwa i cena początkowa. */
public record ProductRequest(String name, BigDecimal price) {
}
