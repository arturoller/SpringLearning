package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do ĆWICZEŃ lekcji Rel04LazyLoading. Celowo puste: dopisz tu zapytania ({@code @Query} z
 * {@code join fetch}, metody z {@code @EntityGraph}), których potrzebujesz w {@link LazyLoadingExercises}.
 */
public interface ExerciseShopOrderRepository extends JpaRepository<ShopOrder, Long> {
}
