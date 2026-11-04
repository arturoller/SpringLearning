package s09_jpa_relations.rel01_many_to_one;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do ĆWICZEŃ lekcji Rel01ManyToOne. Celowo puste: dopisz tu metody (z nazwy albo z {@code @Query}),
 * których potrzebujesz w {@link OrderExercises}. Błąd w nazwie metody albo w JPQL zobaczysz już przy starcie testu.
 */
public interface ExerciseShopOrderRepository extends JpaRepository<ShopOrder, Long> {
}
