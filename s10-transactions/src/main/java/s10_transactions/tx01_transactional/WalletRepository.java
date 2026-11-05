package s10_transactions.tx01_transactional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium portfeli (repository = repozytorium; Spring Data tworzy implementację w czasie działania).
 * UWAGA (sekcja 1 lekcji): KAŻDE wywołanie metody repozytorium spoza transakcji ma WŁASNĄ, krótką transakcję —
 * {@code SimpleJpaRepository} jest oznaczone {@code @Transactional}. Dlatego {@code save} poza transakcją serwisu
 * zatwierdza zmianę od razu.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
