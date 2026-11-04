package s11_testing.test01_test_slices;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium produktów — wycinek {@code @DataJpaTest} tworzy je, wycinek {@code @WebMvcTest} nie. */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
