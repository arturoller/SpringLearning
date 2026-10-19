package s09_jpa_relations.rel06_entity_to_dto;

import java.math.BigDecimal;

/**
 * ProductDto = produkt w API (DTO — Data Transfer Object, obiekt do przenoszenia danych). Rekord (record):
 * niezmienny, bez relacji, bez adnotacji JPA — dokładnie te pola, które chcemy pokazać klientowi API. Ceny zakupu
 * i id z bazy tu NIE ma.
 *
 * <p>Mapowanie „metodą fabryczną” {@code from(...)} w samym DTO — wygodne przy prostych obiektach.</p>
 */
public record ProductDto(String sku, String name, BigDecimal price) {

    public static ProductDto from(Product product) {
        return new ProductDto(product.getSku(), product.getName(), product.getPrice());
    }
}
