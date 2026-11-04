package s09_jpa_relations.rel05_n_plus_one;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do ĆWICZEŃ lekcji Rel05NPlusOne. Celowo puste: dopisz tu zapytania ({@code join fetch},
 * {@code @EntityGraph}, {@code select new}), których potrzebujesz w {@link NPlusOneExercises}.
 */
public interface ExerciseShopOrderRepository extends JpaRepository<ShopOrder, Long> {
}
