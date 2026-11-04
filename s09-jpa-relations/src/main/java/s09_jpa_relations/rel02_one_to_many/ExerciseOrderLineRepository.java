package s09_jpa_relations.rel02_one_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do ĆWICZEŃ lekcji Rel02OneToMany. Celowo puste: dopisz tu zapytania (z nazwy albo {@code @Query}),
 * których potrzebujesz w {@link OrderLineExercises}.
 */
public interface ExerciseOrderLineRepository extends JpaRepository<OrderLine, Long> {
}
