package s09_jpa_relations.rel02_one_to_many;

/** Projekcja-rekord do ćwiczenia 4 lekcji Rel02OneToMany: nazwa produktu i łączna sprzedana ilość. */
public record ProductQuantity(String productName, long quantity) {
}
