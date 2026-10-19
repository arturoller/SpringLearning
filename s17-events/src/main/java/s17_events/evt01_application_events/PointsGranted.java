package s17_events.evt01_application_events;

/** Zdarzenie „przyznano punkty lojalnościowe” — publikuje je Spring, gdy słuchacz zwróci ten rekord (sekcja 5 lekcji). */
public record PointsGranted(String customer, int points) {
}
