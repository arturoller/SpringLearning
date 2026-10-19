package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repozytorium klientów. {@code findWithOrdersById} — klient RAZEM z zamówieniami jednym zapytaniem
 * ({@code left join fetch}: klient bez zamówień też się znajdzie).
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("select c from Customer c left join fetch c.orders where c.id = :id")
    Optional<Customer> findWithOrdersById(Long id);

    Optional<Customer> findByName(String name);
}
