package s10_transactions.tx01_transactional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Account = konto klienta sklepu (portfel na zakupy). Encja (entity — klasa zapisywana jako wiersz tabeli) z kluczem
 * naturalnym: numerem konta nadawanym przez nas, np. "PL-ANNA". Saldo (balance) w groszach — liczby całkowite nie mają
 * błędów zaokrągleń.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String number;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(nullable = false)
    private long balance;

    /** Konstruktor bez argumentów — wymagany przez JPA (Hibernate tworzy nim obiekt przy odczycie). */
    protected Account() {
    }

    public Account(String number, String owner, long balance) {
        this.number = number;
        this.owner = owner;
        this.balance = balance;
    }

    /** Obciąż konto (debit = obciążenie). Za mało pieniędzy → IllegalStateException. */
    public void debit(long amount) {
        if (amount > balance) {
            throw new IllegalStateException("Za mało środków na koncie " + number + ": " + balance + " < " + amount);
        }
        balance -= amount;
    }

    /** Uznaj konto (credit = uznanie, wpływ pieniędzy). */
    public void credit(long amount) {
        balance += amount;
    }

    public String getNumber() {
        return number;
    }

    public String getOwner() {
        return owner;
    }

    public long getBalance() {
        return balance;
    }
}
