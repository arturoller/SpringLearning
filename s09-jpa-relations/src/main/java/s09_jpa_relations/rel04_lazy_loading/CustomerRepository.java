package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium klientów lekcji Rel04LazyLoading. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
