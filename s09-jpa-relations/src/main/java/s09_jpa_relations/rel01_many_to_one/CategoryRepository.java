package s09_jpa_relations.rel01_many_to_one;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium kategorii (repository = repozytorium; szczegóły w s08_data_jpa/Jpa02Repositories). */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
}
