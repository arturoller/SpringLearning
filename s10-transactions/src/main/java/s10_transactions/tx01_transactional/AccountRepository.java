package s10_transactions.tx01_transactional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repozytorium kont (Spring Data JPA tworzy implementację sam). Uwaga: KAŻDA metoda gotowego repozytorium
 * (findById, save, …) jest już transakcyjna sama w sobie — SimpleJpaRepository ma adnotację {@code @Transactional}.
 * Dlatego bez {@code @Transactional} w serwisie każde wywołanie repozytorium to OSOBNA, osobno zatwierdzana transakcja.
 */
public interface AccountRepository extends JpaRepository<Account, String> {
}
