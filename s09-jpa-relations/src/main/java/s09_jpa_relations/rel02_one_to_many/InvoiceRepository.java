package s09_jpa_relations.rel02_one_to_many;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium faktur (sekcja 2 lekcji: jednokierunkowe @OneToMany). */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
