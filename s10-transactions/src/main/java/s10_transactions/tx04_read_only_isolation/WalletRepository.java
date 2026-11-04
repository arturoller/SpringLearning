package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.Optional;

/**
 * Repozytorium portfeli lekcji Tx04ReadOnlyIsolation. Metody z {@code @Lock} (lock = blokada) to sekcja 4:
 * blokowanie PESYMISTYCZNE — Hibernate dopisuje do SELECT-a klauzulę blokującą wiersz do końca transakcji.
 */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    /**
     * {@code PESSIMISTIC_WRITE} (pesymistyczna, do zapisu) → w H2 {@code select … for update}. Nazwa metody to zwykłe
     * zapytanie pochodne (s08_data_jpa/Jpa03QueryMethods): „find … by id”; słowo „ForUpdate” Spring Data pomija,
     * bo stoi między „find” a „By”.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findForUpdateById(Long id);

    /**
     * Jak wyżej, ale z podpowiedzią {@code jakarta.persistence.lock.timeout} = 0 (query hint = podpowiedź dla
     * zapytania; lock timeout = limit czasu czekania na blokadę): „nie czekaj wcale”. Hibernate zamienia ją
     * w dialekcie H2 na {@code for update nowait}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    Optional<Wallet> findForUpdateNoWaitById(Long id);
}
