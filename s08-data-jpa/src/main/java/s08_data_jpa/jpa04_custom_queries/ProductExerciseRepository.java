package s08_data_jpa.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do ĆWICZEŃ lekcji Jpa04CustomQueries — tu dopisujesz metody z {@code @Query}
 * (opis ćwiczeń: na końcu klasy Jpa04CustomQueries). Uwaga: należy do kontekstu lekcji — błąd w JPQL
 * zatrzyma start WSZYSTKICH testów tej lekcji (komunikat „Query validation failed for ...”).
 */
public interface ProductExerciseRepository extends JpaRepository<Product, Long> {

    // TODO ĆWICZENIA 1–4: metody z @Query (JPQL albo natywne), projekcja na rekord StockValue, @Modifying
}
