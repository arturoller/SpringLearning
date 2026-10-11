package s08_data_jpa.jpa02_repositories;

import org.springframework.data.repository.ListCrudRepository;

/**
 * ProductRepository — skromniejszy interfejs: {@code ListCrudRepository} (CRUD zwracający List zamiast Iterable),
 * bez stronicowania i bez metod specyficznych dla JPA (flush, saveAndFlush, getReferenceById). Wystarczy, gdy
 * potrzebujesz tylko podstawowych operacji — i nie wiąże kodu z JPA.
 */
public interface ProductRepository extends ListCrudRepository<Product, String> {
}
