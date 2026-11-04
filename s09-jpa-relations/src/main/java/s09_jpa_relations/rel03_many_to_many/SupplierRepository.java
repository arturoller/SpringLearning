package s09_jpa_relations.rel03_many_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium dostawców lekcji Rel03ManyToMany. */
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
