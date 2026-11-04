package s09_jpa_relations.rel06_entity_to_dto;

import java.math.BigDecimal;

/** DTO pozycji zamówienia: nazwa produktu zamiast całego produktu, bez odnośnika do zamówienia (brak cyklu). */
public record OrderLineDto(String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

    public static OrderLineDto from(OrderLine line) {
        return new OrderLineDto(line.getProduct().getName(), line.getQuantity(), line.getUnitPrice(), line.lineTotal());
    }
}
