package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Connection;

/**
 * Sekcja 1 lekcji Tx04ReadOnlyIsolation: co NAPRAWDĘ robi {@code @Transactional(readOnly = true)} (read-only = tylko
 * do odczytu) w aplikacji JPA — i czego NIE robi. Każda metoda zwraca „zdjęcie” stanu z wnętrza transakcji.
 */
@Service
public class ReadOnlyService {

    private final WalletRepository wallets;
    private final WalletWriter writer;
    private final EntityManager em; // współdzielony pośrednik — w transakcji wskazuje jej EntityManager
    private final JdbcClient jdbc;

    public ReadOnlyService(WalletRepository wallets, WalletWriter writer, EntityManager em, JdbcClient jdbc) {
        this.wallets = wallets;
        this.writer = writer;
        this.em = em;
        this.jdbc = jdbc;
    }

    /**
     * Zmienia właściciela encji w transakcji TYLKO DO ODCZYTU. Zmiana jest widoczna w obiekcie (w pamięci), ale NIE
     * trafia do bazy: brak flush przy commicie (FlushMode MANUAL) i encja wczytana jako read-only (bez kopii stanu
     * do porównywania — dirty checking jej nie dotyczy).
     */
    @Transactional(readOnly = true)
    public ReadOnlyView renameInReadOnly(long walletId, String newOwner) {
        Wallet wallet = wallets.findById(walletId).orElseThrow();
        wallet.rename(newOwner);
        Session session = em.unwrap(Session.class); // unwrap = odpakuj: dostęp do API Hibernate pod JPA
        return new ReadOnlyView(
                wallet.getOwner(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly(),
                session.getHibernateFlushMode().name(),
                session.isDefaultReadOnly(),
                session.isReadOnly(wallet),
                session.doReturningWork(Connection::isReadOnly)); // doReturningWork = wykonaj pracę na połączeniu JDBC
    }

    /** Jak wyżej, ale z jawnym {@code em.flush()} — sprawdzamy, czy „wymuszony” zapis coś zmieni. */
    @Transactional(readOnly = true)
    public void renameAndFlushInReadOnly(long walletId, String newOwner) {
        wallets.findById(walletId).orElseThrow().rename(newOwner);
        em.flush(); // flush = wypchnij zmiany z kontekstu utrwalania do bazy
    }

    /**
     * Jawny UPDATE przez JdbcClient w transakcji read-only. readOnly to WSKAZÓWKA ({@code Connection.setReadOnly(true)}),
     * a H2 ją ignoruje — UPDATE przechodzi i zostaje zatwierdzony (test). Inne bazy (np. PostgreSQL) odrzuciłyby taki
     * zapis, więc nie polegaj na żadnym z tych zachowań.
     */
    @Transactional(readOnly = true)
    public int renameWithJdbcInReadOnly(long walletId, String newOwner) {
        return jdbc.sql("UPDATE wallet SET owner = ? WHERE id = ?").param(newOwner).param(walletId).update();
    }

    /**
     * Sekcja 5 (pułapka): metoda read-only woła „piszącą” metodę innego beana. {@link WalletWriter#rename} ma zwykłe
     * {@code @Transactional} (REQUIRED), więc DOŁĄCZA do tej transakcji — razem z jej trybem read-only. Zmiana przepada
     * bez żadnego wyjątku.
     */
    @Transactional(readOnly = true)
    public void renameThroughWriter(long walletId, String newOwner) {
        writer.rename(walletId, newOwner);
    }

    /**
     * Stan z wnętrza transakcji read-only.
     *
     * @param ownerInMemory        właściciel w obiekcie encji (po zmianie)
     * @param transactionReadOnly  {@code TransactionSynchronizationManager.isCurrentTransactionReadOnly()}
     * @param flushMode            tryb wypychania zmian sesji Hibernate ({@code FlushMode})
     * @param sessionDefaultReadOnly czy sesja wczytuje encje domyślnie jako read-only
     * @param entityReadOnly       czy TA encja jest read-only
     * @param connectionReadOnly   {@code Connection.isReadOnly()} połączenia JDBC transakcji
     */
    public record ReadOnlyView(String ownerInMemory, boolean transactionReadOnly, String flushMode,
                               boolean sessionDefaultReadOnly, boolean entityReadOnly, boolean connectionReadOnly) {
    }
}
