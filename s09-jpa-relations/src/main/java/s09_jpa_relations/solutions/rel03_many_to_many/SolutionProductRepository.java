package s09_jpa_relations.solutions.rel03_many_to_many;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s09_jpa_relations.rel03_many_to_many.Product;

import java.util.Collection;
import java.util.List;

/** Zapytania do rozwiązań wzorcowych lekcji Rel03ManyToMany (odpowiednik ExerciseProductRepository). */
public interface SolutionProductRepository extends JpaRepository<Product, Long> {

    // Rozwiązanie 3: distinct — nazwy (wartości, nie encje) bez niego powtarzają się dla każdego pasującego tagu
    @Query("select distinct p.name from Product p join p.tags t where t.name in :tagNames order by p.name")
    List<String> findNamesWithAnyTag(Collection<String> tagNames);

    // Rozwiązanie 4: „ma wszystkie” = liczba pasujących tagów w grupie równa liczbie szukanych tagów
    @Query("""
            select p.name from Product p join p.tags t
            where t.name in :tagNames
            group by p.name
            having count(t) = :tagCount
            order by p.name""")
    List<String> findNamesWithAllTags(Collection<String> tagNames, long tagCount);
}
