package s17_events.evt01_application_events;

/** Zdarzenie, na które pierwszy słuchacz zawsze rzuca wyjątek (sekcja 3 lekcji: wyjątek słuchacza wraca do publikującego). */
public record BrokenEvent(String reason) {
}
