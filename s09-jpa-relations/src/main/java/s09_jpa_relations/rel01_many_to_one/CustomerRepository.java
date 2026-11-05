package s09_jpa_relations.rel01_many_to_one;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repozytorium klientów lekcji Rel01ManyToOne. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);
}
