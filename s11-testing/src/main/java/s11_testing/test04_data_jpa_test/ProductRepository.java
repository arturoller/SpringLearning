package s11_testing.test04_data_jpa_test;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repozytorium produktów. Metod z {@code JpaRepository} (save, findById…) NIE testujemy — działają. Testujemy to, co
 * napisaliśmy sami: własne zapytanie JPQL {@link #findLowStock(int)} i metodę z projekcją.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Produkty, których zostało co najwyżej {@code threshold} sztuk — od najmniejszego stanu, potem po nazwie. */
    @Query("select p from Product p where p.stock <= :threshold order by p.stock, p.name")
    List<Product> findLowStock(@Param("threshold") int threshold);

    /** Nazwa i cena produktów z kategorii o podanej nazwie, posortowane po nazwie (zapytanie z nazwy metody + projekcja). */
    List<ProductSummary> findByCategoryNameOrderByName(String categoryName);
}
