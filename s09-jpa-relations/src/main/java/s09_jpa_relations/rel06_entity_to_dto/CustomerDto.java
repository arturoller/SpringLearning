package s09_jpa_relations.rel06_entity_to_dto;

import java.util.List;

/**
 * CustomerDto = klient w API: dane kontaktowe i lista zamówień jako proste rekordy {@link OrderDto}.
 * Zamówienie w DTO NIE zna klienta — drzewo zamiast grafu z cyklem, więc nie ma czego zapętlić w JSON.
 * Brak internalNote (notatki wewnętrznej) i id z bazy — API pokazuje tylko to, co postanowiliśmy pokazać.
 */
public record CustomerDto(String name, String city, String email, List<OrderDto> orders) {

    /** Zamówienie w API — numer i status. */
    public record OrderDto(String number, String status) {
    }
}
