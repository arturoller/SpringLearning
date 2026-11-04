package s10_transactions.tx01_transactional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Portfel klienta (wallet = portfel) lekcji Tx01Transactional. Kwoty w {@code BigDecimal} (dokładne liczby
 * dziesiętne — nigdy double dla pieniędzy). Identyfikatory nadaje migracja Flyway (V2), więc bez @GeneratedValue.
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

    public Wallet(Long id, String owner, BigDecimal balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }

    /** withdraw = wypłać: odejmuje kwotę od salda (bez sprawdzania — to zadanie serwisu i ograniczenia CHECK w bazie). */
    public void withdraw(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

    /** deposit = wpłać: dodaje kwotę do salda. */
    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
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
