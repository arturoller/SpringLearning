package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * Portfel z blokowaniem OPTYMISTYCZNYM (sekcja 3 lekcji Tx04ReadOnlyIsolation). Pole {@code @Version}
 * (version = wersja) Hibernate obsługuje sam: przy każdym UPDATE dopisuje {@code version = version + 1} i warunek
 * {@code WHERE … AND version = <wersja odczytana>}. Jeśli w międzyczasie ktoś zmienił wiersz, UPDATE trafia w 0 wierszy
 * i Hibernate zgłasza konflikt (Spring: {@code ObjectOptimisticLockingFailureException}).
 */
@Entity
@Table(name = "versioned_wallet")
public class VersionedWallet {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Version // NIE ustawiaj ręcznie — wersję podbija Hibernate przy każdym UPDATE
    private long version;

    protected VersionedWallet() { // dla Hibernate
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public Long getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VersionedWallet other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return VersionedWallet.class.hashCode();
    }

    @Override
    public String toString() {
        return "VersionedWallet[id=" + id + ", balance=" + balance + ", version=" + version + "]";
    }
}
