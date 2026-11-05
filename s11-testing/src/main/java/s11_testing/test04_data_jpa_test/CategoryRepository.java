package s11_testing.test04_data_jpa_test;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium kategorii. */
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
