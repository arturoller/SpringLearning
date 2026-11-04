package s08_data_jpa.jpa02_repositories;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium produktów — CAŁY kod. Nie piszesz implementacji: Spring Data przy starcie tworzy proxy (pośrednika)
 * implementujące ten interfejs, a wywołania przekazuje do gotowej klasy {@code SimpleJpaRepository}.
 * Parametry typu: {@code Product} — encja, {@code Long} — typ jej id.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
