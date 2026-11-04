package s09_jpa_relations.rel04_lazy_loading;

/**
 * Projekcja (projection — tylko wybrane kolumny) zamówienia jako rekord: numer i nazwisko klienta. Wypełnia ją
 * zapytanie {@code select new …} (ShopOrderRepository.findViewByOrderNumber) — to nie encja, więc nie ma w niej
 * nic leniwego, co mogłoby wybuchnąć poza transakcją (sekcja 3, sposób d).
 */
public record OrderView(String orderNumber, String customerName) {
}
