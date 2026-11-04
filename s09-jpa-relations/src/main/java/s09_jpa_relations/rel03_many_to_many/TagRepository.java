package s09_jpa_relations.rel03_many_to_many;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Repozytorium tagów lekcji Rel03ManyToMany. */
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    // Liczba produktów na tag. LEFT JOIN — żeby tag bez produktów ("nowość") też był w wyniku, z liczbą 0.
    @Query("select t.name, count(p) from Tag t left join t.products p group by t.name order by t.name")
    List<TagCount> countProductsPerTag();
}
