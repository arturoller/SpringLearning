package s10_transactions.tx05_programmatic_transactions;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Sekcja 4 lekcji Tx05ProgrammaticTransactions: akcje PO transakcji. {@code TransactionSynchronization}
 * (synchronizacja transakcji — „słuchacz” zdarzeń jednej transakcji) rejestrujemy w trwającej transakcji; menedżer
 * wywoła jego metody w odpowiednim momencie: {@code beforeCommit}, {@code afterCommit} (tylko po udanym commicie),
 * {@code afterCompletion(status)} (zawsze — po commicie albo rollbacku).
 */
@Service
public class OrderNotifications {

    private final JdbcClient jdbc;
    private final FakeEmailSender mail;
    private final List<String> completions = new ArrayList<>();
    private AfterCommitView lastAfterCommitView;

    public OrderNotifications(JdbcClient jdbc, FakeEmailSender mail) {
        this.jdbc = jdbc;
        this.mail = mail;
    }

    /** PRZED: e-mail wysłany W ŚRODKU transakcji — gdy transakcja się potem wycofa, e-mail i tak już wyszedł. */
    @Transactional
    public void placeOrderSendingMailImmediately(OrderLine line, boolean failAtEnd) {
        OrderSql.insertOrder(jdbc, line, "NEW");
        mail.send("klient@example.com", "Przyjęliśmy zamówienie " + line.number());
        failIf(failAtEnd);
    }

    /** PO: e-mail dopiero w {@code afterCommit} — po wycofaniu transakcji nie wychodzi wcale. */
    @Transactional
    public void placeOrderSendingMailAfterCommit(OrderLine line, boolean failAtEnd) {
        OrderSql.insertOrder(jdbc, line, "NEW");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { // after commit = po zatwierdzeniu
                mail.send("klient@example.com", "Przyjęliśmy zamówienie " + line.number());
            }

            @Override
            public void afterCompletion(int status) { // after completion = po zakończeniu (commit albo rollback)
                recordCompletion(line.number() + ": " + (status == STATUS_COMMITTED ? "COMMITTED"
                        : status == STATUS_ROLLED_BACK ? "ROLLED_BACK" : "UNKNOWN"));
            }
        });
        failIf(failAtEnd);
    }

    /**
     * PUŁAPKA: wyjątek w {@code afterCommit} (np. serwer poczty nie odpowiada). Commit JUŻ się odbył — zamówienie jest
     * w bazie — a wywołujący i tak dostaje wyjątek (test). Kto go złapie i „ponowi zamówienie”, zrobi duplikat.
     */
    @Transactional
    public void placeOrderWithFailingMail(OrderLine line) {
        OrderSql.insertOrder(jdbc, line, "NEW");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                throw new IllegalStateException("Serwer poczty nie odpowiada");
            }
        });
    }

    /**
     * PUŁAPKA: zapis do bazy w {@code afterCommit}. Transakcja jest już zatwierdzona, a zasoby (połączenie) wciąż są
     * przypięte do wątku — {@code isActualTransactionActive()} nadal zwraca true. Zapamiętujemy, co widać w tym momencie
     * (stan transakcji i tryb auto-commit połączenia — {@link #lastAfterCommitView()}), potem robimy INSERT do
     * audit_log. Metoda nie może tego zwrócić: afterCommit dzieje się PO jej powrocie (commit robi proxy).
     */
    @Transactional
    public void placeOrderAndAuditInAfterCommit(OrderLine line) {
        OrderSql.insertOrder(jdbc, line, "NEW");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rememberAfterCommitView(new AfterCommitView(TxState.now(),
                        jdbc.sql("SELECT AUTOCOMMIT()").query(Boolean.class).single())); // AUTOCOMMIT() — funkcja H2
                OrderSql.insertAudit(jdbc, "Wysłano potwierdzenie " + line.number());
            }
        });
    }

    /** PUŁAPKA: rejestracja bez trwającej transakcji (brak {@code @Transactional}). */
    public void registerWithoutTransaction() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        });
    }

    public synchronized AfterCommitView lastAfterCommitView() {
        return lastAfterCommitView;
    }

    private synchronized void rememberAfterCommitView(AfterCommitView view) {
        this.lastAfterCommitView = view;
    }

    public synchronized List<String> completions() {
        return List.copyOf(completions);
    }

    public synchronized void clearCompletions() {
        completions.clear();
    }

    private synchronized void recordCompletion(String entry) {
        completions.add(entry);
    }

    /** Co widać w afterCommit: stan transakcji w wątku i czy połączenie jest w trybie auto-commit. */
    public record AfterCommitView(TxState state, boolean autoCommit) {
    }

    private static void failIf(boolean fail) {
        if (fail) {
            throw new IllegalStateException("Błąd po zapisie zamówienia — transakcja zostanie wycofana");
        }
    }
}
