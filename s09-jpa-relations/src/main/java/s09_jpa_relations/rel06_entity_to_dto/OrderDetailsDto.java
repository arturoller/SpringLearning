package s09_jpa_relations.rel06_entity_to_dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO szczegółów zamówienia: dane zamówienia + lista {@link OrderLineDto}. Zbudowany przez {@link OrderMapper}
 * w transakcji serwisu z encji wczytanej jednym zapytaniem ({@code join fetch}).
 */
public record OrderDetailsDto(String orderNumber, String customerName, OrderStatus status, String note,
                              List<OrderLineDto> lines, BigDecimal total) {
}
