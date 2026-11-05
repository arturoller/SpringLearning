package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Portfel klienta (wallet = portfel) lekcji Tx04ReadOnlyIsolation — BEZ kolumny wersji. Na nim widać lost update
 * (sekcja 3) i blokadę pesymistyczną (sekcja 4). Identyfikatory nadaje migracja Flyway (V2).
 */
@Entity // entity = encja: obiekt odwzorowany na wiersz tabeli
@Table(name = "wallet")
public class Wallet {

    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String owner; // owner = właściciel

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance; // balance = saldo

    protected Wallet() { // dla Hibernate
    }

    /** deposit = wpłać: dodaje kwotę do salda. */
    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    /** withdraw = wypłać: odejmuje kwotę od salda (sprawdzenie salda robi serwis). */
    public void withdraw(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    public void rename(String newOwner) {
        this.owner = newOwner;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Wallet other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Wallet.class.hashCode();
    }

    @Override
    public String toString() {
        return "Wallet[id=" + id + ", owner=" + owner + ", balance=" + balance + "]";
    }
}
