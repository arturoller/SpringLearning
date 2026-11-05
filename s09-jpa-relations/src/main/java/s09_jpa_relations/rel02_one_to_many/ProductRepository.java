package s09_jpa_relations.rel02_one_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium produktów lekcji Rel02OneToMany. */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
