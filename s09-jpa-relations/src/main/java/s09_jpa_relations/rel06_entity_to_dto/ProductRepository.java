package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium produktów lekcji Rel06EntityToDto ({@code findAllById} — wszystkie produkty żądania jednym zapytaniem). */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
