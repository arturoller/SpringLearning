package s10_transactions.tx04_read_only_isolation;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repozytorium portfeli z wersją (blokowanie optymistyczne — sekcja 3). */
public interface VersionedWalletRepository extends JpaRepository<VersionedWallet, Long> {
}
