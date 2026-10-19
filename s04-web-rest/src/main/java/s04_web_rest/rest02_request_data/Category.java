package s04_web_rest.rest02_request_data;

/**
 * Category = kategoria produktu. Spring zamienia tekst z adresu ("ELEKTRONIKA") na stałą wyliczenia (enum) sam —
 * ale tylko przy DOKŁADNEJ zgodności nazwy (wielkość liter ma znaczenie: "elektronika" → 400).
 */
public enum Category {
    ELEKTRONIKA, SPOZYWCZE, KSIAZKI, ODZIEZ, DOM
}
