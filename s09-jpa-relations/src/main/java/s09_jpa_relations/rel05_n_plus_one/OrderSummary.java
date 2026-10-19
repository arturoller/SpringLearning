package s09_jpa_relations.rel05_n_plus_one;

/**
 * OrderSummary = podsumowanie zamówienia — projekcja DTO (DTO = Data Transfer Object, obiekt do przenoszenia
 * danych). Rekord wypełniany bezpośrednio przez zapytanie {@code select new ...OrderSummary(...)}: tylko potrzebne
 * kolumny, jedno zapytanie, ŻADNYCH encji w kontekście utrwalania (sekcja 6 lekcji).
 */
public record OrderSummary(String number, String customerName, String city) {
}
