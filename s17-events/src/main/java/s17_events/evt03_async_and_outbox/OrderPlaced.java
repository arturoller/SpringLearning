package s17_events.evt03_async_and_outbox;

import java.math.BigDecimal;

/** Zdarzenie „zamówienie złożone” tej lekcji; obsługuje je słuchacz wysyłający powiadomienie BEZPOŚREDNIO po commicie. Kwota w zł. */
public record OrderPlaced(String orderId, String customer, BigDecimal totalPln) {
}
