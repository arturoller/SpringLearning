package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium klientów lekcji Rel06EntityToDto. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
