package s17_events.evt02_transactional_events;

import java.math.BigDecimal;

/** Zdarzenie „zamówienie złożone” tej lekcji (każda lekcja ma własne rekordy). Kwota w złotych. */
public record OrderPlaced(String orderId, String customer, BigDecimal totalPln) {
}
