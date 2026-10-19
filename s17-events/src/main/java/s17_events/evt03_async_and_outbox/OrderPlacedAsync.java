package s17_events.evt03_async_and_outbox;

import java.math.BigDecimal;

/** Zdarzenie „zamówienie złożone” obsługiwane ASYNCHRONICZNIE po zatwierdzeniu transakcji (sekcja 3 lekcji). Kwota w zł. */
public record OrderPlacedAsync(String orderId, String customer, BigDecimal totalPln) {
}
