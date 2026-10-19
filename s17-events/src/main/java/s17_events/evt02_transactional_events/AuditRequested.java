package s17_events.evt02_transactional_events;

/** Zdarzenie „zapisz audyt”; pole {@code failIn} mówi, w której fazie słuchacz ma rzucić wyjątek (BEFORE_COMMIT lub AFTER_COMMIT). */
public record AuditRequested(String orderId, String failIn) {
}
