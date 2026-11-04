package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sekcja 2 lekcji Tx04ReadOnlyIsolation: poziomy izolacji (isolation level = poziom izolacji — jak bardzo transakcja
 * jest „odgrodzona” od innych) i zjawiska, które dopuszczają. Odczyty idą przez JdbcClient, czyli prosto do bazy.
 */
@Service
public class IsolationShowcase {

    private final JdbcClient jdbc;
    private final OtherUser otherUser;
    private final WalletWriter writer;
    private final EntityManager em;

    public IsolationShowcase(JdbcClient jdbc, OtherUser otherUser, WalletWriter writer, EntityManager em) {
        this.jdbc = jdbc;
        this.otherUser = otherUser;
        this.writer = writer;
        this.em = em;
    }

    /** Poziom izolacji w transakcji z domyślnym {@code Isolation.DEFAULT} = to, co ustawia baza (H2: READ COMMITTED). */
    @Transactional
    public String defaultIsolation() {
        return jdbc.sql(WalletWriter.ISOLATION_SQL).query(String.class).single();
    }

    /** Spring ustawia poziom na połączeniu ({@code Connection.setTransactionIsolation}) — baza go potwierdza. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String repeatableReadIsolation() {
        return jdbc.sql(WalletWriter.ISOLATION_SQL).query(String.class).single();
    }

    /** Non-repeatable read (niepowtarzalny odczyt): READ COMMITTED — drugi odczyt widzi cudzy commit. */
    @Transactional
    public List<BigDecimal> readBalanceTwice(long walletId, Runnable meanwhile) {
        BigDecimal first = balance(walletId);
        meanwhile.run();
        return List.of(first, balance(walletId));
    }

    /** REPEATABLE READ (powtarzalny odczyt): oba odczyty dają to samo, choć ktoś w międzyczasie zatwierdził zmianę. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<BigDecimal> readBalanceTwiceRepeatable(long walletId, Runnable meanwhile) {
        BigDecimal first = balance(walletId);
        meanwhile.run();
        return List.of(first, balance(walletId));
    }

    /**
     * PUŁAPKA: ten sam scenariusz przez JPA. Drugie {@code em.find} nie idzie do bazy — zwraca obiekt z kontekstu
     * utrwalania (pamięci podręcznej pierwszego poziomu). Wynik „powtarzalny” NIE wynika z izolacji bazy.
     */
    @Transactional
    public List<BigDecimal> readBalanceTwiceWithJpa(long walletId, Runnable meanwhile) {
        BigDecimal first = em.find(Wallet.class, walletId).getBalance();
        meanwhile.run();
        return List.of(first, em.find(Wallet.class, walletId).getBalance());
    }

    /**
     * Dirty read (brudny odczyt): ta transakcja zmienia saldo na 999.00 i NIE zatwierdza. W tym czasie „drugi użytkownik”
     * czyta saldo dwa razy: z READ_COMMITTED i z READ_UNCOMMITTED. Na końcu wycofujemy zmianę — kto widział 999.00,
     * widział dane, których nigdy nie było.
     */
    @Transactional
    public DirtyReadView dirtyRead(long walletId) {
        jdbc.sql("UPDATE wallet SET balance = 999.00 WHERE id = ?").param(walletId).update();
        DirtyReadView view = new DirtyReadView(otherUser.readBalance(walletId), otherUser.readBalanceUncommitted(walletId));
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // wycofaj na końcu (Tx01, sekcja 4)
        return view;
    }

    /** Sekcja 5 (pułapka): metoda z SERIALIZABLE wywołana z tej transakcji (REQUIRED) dołącza do niej. */
    @Transactional
    public String isolationSeenByJoiningSerializableMethod() {
        return writer.isolationSeenBySerializableMethod();
    }

    private BigDecimal balance(long walletId) {
        return jdbc.sql("SELECT balance FROM wallet WHERE id = ?").param(walletId).query(BigDecimal.class).single();
    }

    /** Co zobaczył drugi użytkownik w czasie, gdy saldo było zmienione, ale niezatwierdzone. */
    public record DirtyReadView(BigDecimal seenWithReadCommitted, BigDecimal seenWithReadUncommitted) {
    }
}
