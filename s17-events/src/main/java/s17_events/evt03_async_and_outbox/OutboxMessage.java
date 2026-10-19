package s17_events.evt03_async_and_outbox;

/** Wiadomość z tabeli outbox (outbox = skrzynka nadawcza): co wysłać, o którym zamówieniu, ile razy już próbowano. */
public record OutboxMessage(long id, String orderId, String type, String payload, int attempts) {
}
