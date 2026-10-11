package s10_transactions.tx02_propagation;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium wpisów audytu (Spring Data JPA). */
public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {
}
