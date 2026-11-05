package s09_jpa_relations.rel06_entity_to_dto;

import java.math.BigDecimal;

/**
 * DTO wiersza listy zamówień: numer, klient, status i wartość — bez pozycji. Wypełnia go wprost zapytanie
 * {@code select new …} (ShopOrderRepository.findSummaries): 1 zapytanie, 0 encji (s09_jpa_relations/Rel05NPlusOne).
 */
public record OrderSummaryDto(String orderNumber, String customerName, OrderStatus status, BigDecimal total) {
}
