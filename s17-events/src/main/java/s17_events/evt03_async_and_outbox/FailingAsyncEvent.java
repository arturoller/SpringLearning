package s17_events.evt03_async_and_outbox;

/** Zdarzenie, na które asynchroniczny słuchacz zawsze rzuca wyjątek (sekcja 2 lekcji: kto go zobaczy?). */
public record FailingAsyncEvent(String reason) {
}
