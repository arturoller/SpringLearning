package s04_web_rest.rest04_json;

/**
 * CustomerJson = klient w JSON-ie — SZKIELET ćwiczenia 1 lekcji Rest04Json. Rekord nie ma jeszcze adnotacji Jacksona:
 * dopisz je (opis w Rest04Json.exercise1). Rozwiązanie: SolutionCustomerJson w pakiecie solutions.
 */
public record CustomerJson(
        String fullName,        // TODO: w JSON-ie klucz "full_name"
        String email,           // TODO: pomijany, gdy null
        String password) {      // TODO: nigdy nie trafia do JSON-a
}
