package s10_transactions.solutions.tx02_propagation;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Rozwiązania ćwiczeń 1 i 3 lekcji Tx02Propagation. */
@Service
public class SolutionAuditLog {

    private final JdbcClient jdbc;

    public SolutionAuditLog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** ĆWICZENIE 1: REQUIRES_NEW — własna transakcja, commit od razu; rollback wywołującego go nie dotyczy. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String message) {
        jdbc.sql("INSERT INTO audit_log (message) VALUES (?)").param(message).update();
    }

    /** ĆWICZENIE 3: MANDATORY — bez trwającej transakcji proxy rzuca IllegalTransactionStateException przed metodą. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void postToLedger(String entry) {
        jdbc.sql("INSERT INTO audit_log (message) VALUES (?)").param("KSIĘGA: " + entry).update();
    }
}
