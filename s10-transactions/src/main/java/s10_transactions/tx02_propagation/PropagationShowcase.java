package s10_transactions.tx02_propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sekcja 4 lekcji Tx02Propagation: pozostałe rodzaje propagacji. Każda metoda tylko zwraca stan transakcji, który
 * widzi — testy wołają je raz BEZ transakcji, raz WEWNĄTRZ transakcji (TransactionTemplate).
 */
@Service
public class PropagationShowcase {

    /** SUPPORTS (wspieraj): jest transakcja — dołącz; nie ma — działaj bez niej. */
    @Transactional(propagation = Propagation.SUPPORTS)
    public TxState supports() {
        return TxState.now();
    }

    /** NOT_SUPPORTED (nie wspieraj): zawsze BEZ transakcji; trwająca jest na ten czas zawieszana. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TxState notSupported() {
        return TxState.now();
    }

    /** MANDATORY (obowiązkowa): musi już trwać transakcja; bez niej — IllegalTransactionStateException. */
    @Transactional(propagation = Propagation.MANDATORY)
    public TxState mandatory() {
        return TxState.now();
    }

    /** NEVER (nigdy): nie może trwać transakcja; gdy trwa — IllegalTransactionStateException. */
    @Transactional(propagation = Propagation.NEVER)
    public TxState never() {
        return TxState.now();
    }
}
