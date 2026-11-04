package s09_jpa_relations.rel01_many_to_one;

/** Projekcja-rekord do ćwiczenia 4 lekcji Rel01ManyToOne: imię i nazwisko klienta oraz liczba jego zamówień. */
public record CustomerOrderCount(String customerName, long orderCount) {
}
