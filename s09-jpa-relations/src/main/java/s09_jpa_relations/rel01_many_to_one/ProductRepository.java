package s09_jpa_relations.rel01_many_to_one;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repozytorium produktów. findByCategoryName — metoda zapytaniowa (query method), która „przechodzi” przez relację:
 * Category + Name → właściwość category.name. Spring Data buduje z niej zapytanie ze złączeniem (join) z categories.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryNameOrderByName(String categoryName);
}
