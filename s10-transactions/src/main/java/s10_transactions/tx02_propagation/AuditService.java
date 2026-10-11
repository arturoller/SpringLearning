package s10_transactions.tx02_propagation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * AuditService = serwis dziennika audytu. Każda metoda ma INNĄ propagację (propagation = rozprzestrzenianie się —
 * „co zrobić, gdy wołający JUŻ ma transakcję, a co, gdy nie ma”). Metody są wywoływane z OrderService (inny bean),
 * więc wywołanie przechodzi przez proxy i propagacja naprawdę działa.
 */
@Service
public class AuditService {

    private final AuditEntryRepository auditLog;

    public AuditService(AuditEntryRepository auditLog) {
        this.auditLog = auditLog;
    }

    /** REQUIRED (wymagana, domyślna): jest transakcja → DOŁĄCZ do niej; nie ma → utwórz nową. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logRequired(String message) {
        auditLog.save(new AuditEntry(message));
    }

    /** REQUIRES_NEW (wymaga nowej): ZAWSZE nowa, niezależna transakcja; bieżąca jest na ten czas wstrzymana. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRequiresNew(String message) {
        auditLog.save(new AuditEntry(message));
    }

    /** MANDATORY (obowiązkowa): musi już trwać transakcja; brak → IllegalTransactionStateException. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void logMandatory(String message) {
        auditLog.save(new AuditEntry(message));
    }

    /** NESTED (zagnieżdżona): wewnątrz istniejącej transakcji tworzy PUNKT ZAPISU (savepoint) i wraca do niego przy błędzie. */
    @Transactional(propagation = Propagation.NESTED)
    public void logNested(String message) {
        auditLog.save(new AuditEntry(message));
    }

    // --- wersje, które zapisują wpis i od razu się wywracają ---------------------------------------------------

    /** Zapisuje wpis i rzuca wyjątek — w transakcji REQUIRED (czyli w transakcji WOŁAJĄCEGO). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logRequiredAndFail(String message) {
        auditLog.save(new AuditEntry(message));
        throw new IllegalStateException("Audyt nie przyjął wpisu: " + message);
    }

    /** Zapisuje wpis i rzuca wyjątek — we WŁASNEJ transakcji REQUIRES_NEW. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRequiresNewAndFail(String message) {
        auditLog.save(new AuditEntry(message));
        throw new IllegalStateException("Audyt nie przyjął wpisu: " + message);
    }

    /** Zapisuje wpis i rzuca wyjątek — w transakcji zagnieżdżonej NESTED (punkt zapisu). */
    @Transactional(propagation = Propagation.NESTED)
    public void logNestedAndFail(String message) {
        auditLog.save(new AuditEntry(message));
        throw new IllegalStateException("Audyt nie przyjął wpisu: " + message);
    }

    // --- metody-„sondy”: czy w środku trwa transakcja i jak się nazywa ----------------------------------------

    /** SUPPORTS (wspiera): jest transakcja → użyj jej; nie ma → działaj BEZ transakcji. */
    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean supportsSeesTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** NOT_SUPPORTED (nie wspiera): jest transakcja → WSTRZYMAJ ją i działaj bez transakcji. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean notSupportedSeesTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** NEVER (nigdy): jest transakcja → IllegalTransactionStateException; nie ma → działaj bez transakcji. */
    @Transactional(propagation = Propagation.NEVER)
    public boolean neverSeesTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Nazwa bieżącej transakcji przy REQUIRED — dołącza, więc to nazwa transakcji WOŁAJĄCEGO. */
    @Transactional(propagation = Propagation.REQUIRED)
    public String transactionNameRequired() {
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }

    /** Nazwa bieżącej transakcji przy REQUIRES_NEW — nowa transakcja, więc nazwa TEJ metody. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String transactionNameRequiresNew() {
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }
}
