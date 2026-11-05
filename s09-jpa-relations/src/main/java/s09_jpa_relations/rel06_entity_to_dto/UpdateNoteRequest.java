package s09_jpa_relations.rel06_entity_to_dto;

/** Żądanie zmiany uwagi do zamówienia (sekcja 4) — jedno pole, które klient może zmienić. */
public record UpdateNoteRequest(String note) {
}
