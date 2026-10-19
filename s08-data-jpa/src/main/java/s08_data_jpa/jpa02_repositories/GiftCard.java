package s08_data_jpa.jpa02_repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Karta podarunkowa (gift card) — SZKIELET do ćwiczenia 3 lekcji Jpa02Repositories. Kod karty nadaje sklep
 * (np. {@code "KARTA-100"}), więc — jak DiscountCode — każdy save zaczyna się od zbędnego SELECT.
 *
 * <p>Cel ćwiczenia: niech Spring Data sam wie, że karta jest nowa. Zaimplementuj
 * {@code Persistable<String>} (persistable = dający się utrwalić; interfejs Spring Data z metodami
 * {@code getId()} i {@code isNew()}). Wtedy SimpleJpaRepository zamiast sprawdzać id zapyta {@code isNew()}.</p>
 */
@Entity
@Table(name = "gift_cards")
// TODO ĆWICZENIE 3: implements Persistable<String>
public class GiftCard {

    @Id
    private String code;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    // TODO ĆWICZENIE 3: pole @Transient (nie zapisywane w tabeli) z flagą „nowa”, domyślnie true

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

    // TODO ĆWICZENIE 3: getId() i isNew() z interfejsu Persistable
    // TODO ĆWICZENIE 3: metoda z @PostPersist i @PostLoad, która ustawia flagę na false
    //                   (po zapisie i po wczytaniu z bazy karta nie jest już nowa)
}
