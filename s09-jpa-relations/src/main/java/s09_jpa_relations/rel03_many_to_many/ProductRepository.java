package s09_jpa_relations.rel03_many_to_many;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Repozytorium produktów lekcji Rel03ManyToMany — zapytania po relacji N—N (sekcja 5; SQL sprawdzony testem). */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByName(String name);

    // Metoda z nazwy po kolekcji: Tags → Name. Dwa JOIN-y: przez tabelę łączącą do tabeli tag:
    // select p1_0.id,p1_0.name,p1_0.price from product p1_0 join product_tag t1_0 on p1_0.id=t1_0.product_id
    //   join tag t1_1 on t1_1.id=t1_0.tag_id where t1_1.name=? order by p1_0.name
    List<Product> findByTagsNameOrderByName(String tagName);

    // JPQL z join po kolekcji i warunkiem IN — produkt z DWOMA pasującymi tagami pojawia się DWA razy (sekcja 5).
    @Query("select p from Product p join p.tags t where t.name in :tagNames order by p.name")
    List<Product> findWithAnyTag(Collection<String> tagNames);

    // To samo z distinct — każdy produkt raz (SQL: select distinct …).
    @Query("select distinct p from Product p join p.tags t where t.name in :tagNames order by p.name")
    List<Product> findDistinctWithAnyTag(Collection<String> tagNames);
}
