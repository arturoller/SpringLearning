package s17_events.evt02_transactional_events;

/**
 * Zdarzenie „przyznaj punkty” obsługiwane przez słuchacza, który pisze do bazy po commicie BEZ nowej transakcji.
 * Flaga {@code failAfterWrite} każe słuchaczowi rzucić wyjątek tuż po zapisie (do pokazania, czy zapis da się wycofać).
 */
public record NaivePointsRequested(String orderId, String customer, int points, boolean failAfterWrite) {
}
