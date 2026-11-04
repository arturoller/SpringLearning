package s09_jpa_relations.rel06_entity_to_dto;

import java.util.List;

/**
 * Żądanie złożenia zamówienia — kierunek odwrotny: DTO → encja (sekcja 3). Klient podaje TYLKO, czego chce
 * (id produktu i ilość). Nie ma tu ceny, statusu, numeru zamówienia ani id — to ustala serwer.
 */
public record CreateOrderRequest(Long customerId, String note, List<Item> items) {

    /** Jedna pozycja żądania: który produkt i ile sztuk. */
    public record Item(Long productId, int quantity) {
    }
}
