package s09_jpa_relations.solutions.rel04_lazy_loading;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s09_jpa_relations.rel04_lazy_loading.ShopOrder;

import java.util.List;
import java.util.Optional;

/** Zapytania do rozwiązań wzorcowych ćwiczeń lekcji Rel04LazyLoading (odpowiednik ExerciseShopOrderRepository). */
public interface SolutionShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Ćwiczenie 2: pozycje i ich produkty jednym zapytaniem (dwa join fetch po kolei: o.lines, potem l.product)
    @Query("select o from ShopOrder o join fetch o.lines l join fetch l.product where o.id = :id")
    Optional<ShopOrder> findWithLinesAndProductsById(Long id);

    // Ćwiczenie 3: graf encji na metodzie z nazwy — klient dołączony JOIN-em, bez pisania JPQL
    @EntityGraph(attributePaths = "customer")
    List<ShopOrder> findAllByOrderByOrderNumber();
}
