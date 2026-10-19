package s10_transactions.tx01_transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * TransferService = serwis przelewów między kontami klientów. Każda metoda pokazuje jedną regułę
 * {@code @Transactional} (transactional = transakcyjny) z lekcji Tx01Transactional.
 *
 * <p>Spring nie wstrzykuje do innych beanów TEGO obiektu, tylko jego PROXY (pośrednika), które przed metodą otwiera
 * transakcję, a po niej ją zatwierdza (commit) albo wycofuje (rollback).</p>
 */
@Service
public class TransferService {

    private final AccountRepository accounts;

    public TransferService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    // --- 1. PRZED: bez @Transactional --------------------------------------------------------------------------

    /**
     * Przelew BEZ transakcji w serwisie. Każde wywołanie repozytorium to osobna transakcja: save(from) zostaje
     * zatwierdzone od razu. Gdy konto docelowe nie istnieje, pieniądze znikają z konta źródłowego i nigdzie nie trafiają.
     */
    public void transferWithoutTransaction(String from, String to, long amount) {
        Account source = find(from);
        source.debit(amount);
        accounts.save(source);                 // osobna transakcja → COMMIT już tutaj
        Account target = find(to);             // AccountNotFoundException — ale obciążenie już jest w bazie
        target.credit(amount);
        accounts.save(target);
    }

    // --- 2. PO: @Transactional — wszystko albo nic --------------------------------------------------------------

    /**
     * Przelew w JEDNEJ transakcji. Nie ma save(): encje pobrane w transakcji są ZARZĄDZANE (managed), więc Hibernate
     * sam wykryje zmianę salda (dirty checking — wykrywanie zmian) i wyśle UPDATE przed commitem.
     * Wyjątek niekontrolowany (RuntimeException) → rollback: oba salda zostają bez zmian.
     */
    @Transactional
    public void transfer(String from, String to, long amount) {
        find(from).debit(amount);
        find(to).credit(amount);
    }

    /** Jak transfer, ale na końcu „awaria JVM” — Error (błąd) też wycofuje transakcję. */
    @Transactional
    public void transferThenCrash(String from, String to, long amount) {
        transfer(from, to, amount);            // zwykłe wywołanie w tej samej klasie — już JESTEŚMY w transakcji
        throw new OutOfMemoryError("symulacja awarii: brak pamięci");
    }

    // --- 3. Wyjątek KONTROLOWANY → domyślnie COMMIT -------------------------------------------------------------

    /**
     * Przelew, po którym potwierdzamy go w banku. Gdy bank nie działa, rzucamy WYJĄTEK KONTROLOWANY. PUŁAPKA: domyślnie
     * Spring go „przepuszcza” i ZATWIERDZA transakcję — przelew zostaje w bazie, choć metoda zakończyła się błędem.
     */
    @Transactional
    public void transferAndConfirm(String from, String to, long amount, boolean bankOnline)
            throws PaymentGatewayException {
        transfer(from, to, amount);
        if (!bankOnline) {
            throw new PaymentGatewayException("Bank nie potwierdził przelewu");
        }
    }

    /** To samo, ale z {@code rollbackFor} (wycofaj dla…) — wyjątek kontrolowany też wycofuje transakcję. */
    @Transactional(rollbackFor = PaymentGatewayException.class)
    public void transferAndConfirmOrRollback(String from, String to, long amount, boolean bankOnline)
            throws PaymentGatewayException {
        transfer(from, to, amount);
        if (!bankOnline) {
            throw new PaymentGatewayException("Bank nie potwierdził przelewu");
        }
    }

    // --- 4. noRollbackFor — wyjątek niekontrolowany, ale bez wycofania -----------------------------------------

    /**
     * Opłata za zakupy + wydruk paragonu. Drukarka nie działa → ReceiptPrinterException (RuntimeException), ale dzięki
     * {@code noRollbackFor} (nie wycofuj dla…) obciążenie konta ZOSTAJE zatwierdzone. Wyjątek i tak leci do wołającego.
     */
    @Transactional(noRollbackFor = ReceiptPrinterException.class)
    public void chargeAndPrintReceipt(String number, long amount, boolean printerWorks) {
        find(number).debit(amount);
        if (!printerWorks) {
            throw new ReceiptPrinterException("Brak papieru w drukarce paragonów");
        }
    }

    // --- 5. Jak zajrzeć do środka: TransactionSynchronizationManager --------------------------------------------

    /** Czy w tej metodzie trwa PRAWDZIWA transakcja? (actual transaction active = rzeczywista transakcja aktywna) */
    @Transactional
    public boolean isTransactionActiveInside() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Ta sama sprawdzarka, ale w metodzie BEZ {@code @Transactional} → false. */
    public boolean isTransactionActiveWithoutAnnotation() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * Ta sama sprawdzarka z adnotacją {@code jakarta.transaction.Transactional} (z API JTA, a NIE ze Springa).
     * Spring ją rozumie, ale ma ona INNE atrybuty (rollbackOn, dontRollbackOn, value = TxType) — patrz PUŁAPKA w lekcji.
     */
    @jakarta.transaction.Transactional
    public boolean isTransactionActiveWithJakartaAnnotation() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    private Account find(String number) {
        return accounts.findById(number).orElseThrow(() -> new AccountNotFoundException(number));
    }
}
