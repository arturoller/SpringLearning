package s09_jpa_relations.rel06_entity_to_dto;

/**
 * CustomerContactDto = dane kontaktowe klienta dla działu obsługi — drugi „kształt” tej samej encji Customer
 * (ćwiczenie 2 lekcji). Jedna encja, wiele DTO: każde API dostaje dokładnie to, czego potrzebuje.
 */
public record CustomerContactDto(String name, String contact) {
}
