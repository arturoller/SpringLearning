package s17_events.evt03_async_and_outbox;

/** Zdarzenie, którego asynchroniczny słuchacz czeka na „bramkę” (zatrzask) — po to, by test był deterministyczny, bez sleep. */
public record GatedEvent(String name) {
}
