package s10_transactions.tx01_transactional;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Podgląd „pod maskę” — sekcja 2 lekcji Tx01Transactional: czy transakcja jest aktywna i z jakiego połączenia
 * korzystają JPA i JDBC w jej trakcie.
 */
@Service
public class TransactionInspector {

    private final EntityManager entityManager; // entity manager = zarządca encji (współdzielony pośrednik Springa)
    private final JdbcClient jdbc;             // JdbcClient = klient JDBC (s07_data_jdbc/Jdbc01JdbcClient)

    public TransactionInspector(EntityManager entityManager, JdbcClient jdbc) {
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    /** W transakcji: active = true, name = „s10_transactions.tx01_transactional.TransactionInspector.insideTransaction”. */
    @Transactional
    public TransactionInfo insideTransaction() {
        return TransactionInfo.now();
    }

    /** Bez adnotacji: active = false, name = null. */
    public TransactionInfo outsideTransaction() {
        return TransactionInfo.now();
    }

    /**
     * Numer sesji bazy H2 ({@code SESSION_ID()} — jedno połączenie JDBC = jedna sesja H2) widziany przez JPA
     * (zapytanie natywne przez EntityManager) i przez JdbcClient W TEJ SAMEJ transakcji. Wynik: dwa RÓWNE numery —
     * {@code JpaTransactionManager} udostępnia połączenie swojej transakcji także kodowi JDBC.
     */
    @Transactional
    public List<Integer> sessionIdsSeenByJpaAndJdbc() {
        Integer fromJpa = ((Number) entityManager.createNativeQuery("SELECT SESSION_ID()").getSingleResult()).intValue();
        Integer fromJdbc = jdbc.sql("SELECT SESSION_ID()").query(Integer.class).single();
        return List.of(fromJpa, fromJdbc);
    }
}
