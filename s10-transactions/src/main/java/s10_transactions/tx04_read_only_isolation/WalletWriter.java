package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sekcja 5 lekcji Tx04ReadOnlyIsolation: atrybuty {@code isolation} (izolacja) i {@code timeout} (limit czasu) — oraz
 * to, że przy DOŁĄCZANIU do trwającej transakcji (REQUIRED) są ignorowane.
 */
@Service
public class WalletWriter {

    /** Poziom izolacji BIEŻĄCEJ sesji H2 — prosto z bazy, nie z pamięci Springa. */
    static final String ISOLATION_SQL =
            "SELECT ISOLATION_LEVEL FROM INFORMATION_SCHEMA.SESSIONS WHERE SESSION_ID = SESSION_ID()";

    private final WalletRepository wallets;
    private final EntityManagerFactory emf;
    private final JdbcClient jdbc;

    public WalletWriter(WalletRepository wallets, EntityManagerFactory emf, JdbcClient jdbc) {
        this.wallets = wallets;
        this.emf = emf;
        this.jdbc = jdbc;
    }

    /** Zwykła metoda „pisząca”: zmiana encji, zapis przy commicie (dirty checking). */
    @Transactional
    public void rename(long walletId, String newOwner) {
        wallets.findById(walletId).orElseThrow().rename(newOwner);
    }

    /**
     * Metoda, która „chce” SERIALIZABLE. Wywołana bez transakcji — otwiera własną i baza pokazuje SERIALIZABLE.
     * Wywołana z trwającej transakcji (REQUIRED) — dołącza i jej izolacja jest IGNOROWANA (test).
     */
    @Transactional(isolation = Isolation.SERIALIZABLE) // serializable = szeregowalna
    public String isolationSeenBySerializableMethod() {
        return jdbc.sql(ISOLATION_SQL).query(String.class).single();
    }

    /**
     * {@code timeout = 5} (sekundy): Spring przekazuje limit do transakcji Hibernate — tu go odczytujemy. Prawdziwy
     * EntityManager transakcji bierzemy z {@code EntityManagerHolder} (pojemnik przypięty do wątku przez
     * JpaTransactionManager), bo współdzielony pośrednik {@code EntityManager} nie pozwala sięgać po
     * {@code getTransaction()} („Not allowed to create transaction on shared EntityManager”).
     */
    @Transactional(timeout = 5)
    public Integer hibernateTimeoutSeconds() {
        EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(emf);
        return holder.getEntityManager().unwrap(Session.class).getTransaction().getTimeout();
    }
}
