package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium do ĆWICZEŃ lekcji Rel06EntityToDto. Celowo puste: dopisz tu zapytania (projekcje
 * {@code select new}, {@code join fetch}), których potrzebujesz w {@link DtoExercises}.
 */
public interface ExerciseShopOrderRepository extends JpaRepository<ShopOrder, Long> {
}
