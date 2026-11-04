package s09_jpa_relations.rel06_entity_to_dto;

/** Status zamówienia (NEW = nowe, PAID = opłacone, SHIPPED = wysłane) — zapisywany jako tekst (EnumType.STRING). */
public enum OrderStatus {
    NEW, PAID, SHIPPED
}
