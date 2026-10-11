package s11_testing.test01_test_slices;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ProductRepository = repozytorium produktów. Spring Data JPA tworzy implementację sam (s08_data_jpa/Jpa02Repositories).
 * W wycinku {@code @DataJpaTest} ten bean JEST, w {@code @WebMvcTest} — NIE MA go (test sekcji 2 i 3).
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Produkty, których nazwa zawiera fragment (bez względu na wielkość liter), posortowane po nazwie. */
    List<Product> findByNameContainingIgnoreCaseOrderByNameAsc(String fragment);
}
