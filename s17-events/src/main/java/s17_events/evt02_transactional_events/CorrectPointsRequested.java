package s17_events.evt02_transactional_events;

/**
 * Zdarzenie „przyznaj punkty” obsługiwane przez słuchacza, który pisze do bazy w nowej transakcji (REQUIRES_NEW).
 * Flaga {@code failAfterWrite} każe słuchaczowi rzucić wyjątek tuż po zapisie (do pokazania, że zapis się wycofa).
 */
public record CorrectPointsRequested(String orderId, String customer, int points, boolean failAfterWrite) {
}
