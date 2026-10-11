package s08_data_jpa.jpa02_repositories;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CustomerRepository = repozytorium klientów. Tylko interfejs — implementację (SimpleJpaRepository) i pośrednika
 * (proxy) tworzy Spring Data przy starcie. {@code JpaRepository<Customer, Long>}: typ encji i typ jej id.
 * Dostajemy gotowe: save, saveAll, findById, findAll, count, existsById, deleteById, delete, flush, saveAndFlush...
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
