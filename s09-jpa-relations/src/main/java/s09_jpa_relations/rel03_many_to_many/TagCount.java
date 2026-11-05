package s09_jpa_relations.rel03_many_to_many;

/** Projekcja-rekord (sekcja 5 lekcji): nazwa tagu i liczba produktów z tym tagiem (także 0). */
public record TagCount(String tagName, long productCount) {
}
