package s11_testing.test07_docker_compose;

import java.math.BigDecimal;

/** Product = produkt sklepu (wiersz tabeli {@code product}); cena w złotych. */
public record Product(Long id, String sku, String name, String category, BigDecimal price) {
}
