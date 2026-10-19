package s17_events.evt01_application_events;

import java.math.BigDecimal;

/**
 * Zdarzenie „zamówienie złożone” (event = zdarzenie). To zwykły rekord — nie dziedziczy po {@code ApplicationEvent}
 * (od Springa 4.2 nie trzeba). Zdarzenie opisuje coś, co JUŻ się stało, więc nazywamy je w czasie przeszłym.
 * Kwota w złotych jako {@code BigDecimal}.
 */
public record OrderPlaced(String orderId, String customer, BigDecimal totalPln) {
}
