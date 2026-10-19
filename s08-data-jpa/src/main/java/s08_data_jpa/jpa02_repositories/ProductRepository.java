package s08_data_jpa.jpa02_repositories;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium produktów. CAŁY kod to ta jedna linia: Spring Data przy starcie tworzy obiekt-pośrednika (proxy),
 * który implementuje ten interfejs i przekazuje wywołania do klasy SimpleJpaRepository (save, findById, count…).
 * {@code JpaRepository<Product, Long>}: typ encji i typ jej klucza głównego.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
