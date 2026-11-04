package s09_jpa_relations.rel03_many_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do ĆWICZEŃ lekcji Rel03ManyToMany. Celowo puste: dopisz tu zapytania (z nazwy albo {@code @Query}),
 * których potrzebujesz w {@link ProductTagExercises}.
 */
public interface ExerciseProductRepository extends JpaRepository<Product, Long> {
}
