package s09_jpa_relations.rel05_n_plus_one;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repozytorium klientów — do pokazania {@code @BatchSize} na kolekcji Customer.orders. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByName();
}
