package s08_data_jpa.solutions.jpa02_repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;

/**
 * Rozwiązanie ćwiczenia 3 lekcji Jpa02Repositories: karta sama mówi Spring Data, czy jest nowa.
 * SimpleJpaRepository dla encji Persistable pyta {@link #isNew()} zamiast sprawdzać, czy id == null.
 */
@Entity
@Table(name = "gift_cards")
public class GiftCard implements Persistable<String> {

    @Id
    private String code;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Transient                             // transient = przejściowy: pole tylko w Javie, bez kolumny w tabeli
    private boolean isNew = true;          // obiekt z konstruktora = nowy

    protected GiftCard() {
    }

    public GiftCard(String code, BigDecimal balance) {
        this.code = code;
        this.balance = balance;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public String getId() {
        return code;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    /** Po zapisie (PostPersist) i po wczytaniu z bazy (PostLoad) karta przestaje być nowa → kolejny save = merge. */
    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
