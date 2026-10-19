package s08_data_jpa.jpa02_repositories;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Kod rabatowy (discount code), np. {@code "LATO10"} = 10% zniżki. Klucz główny nadaje SKLEP (marketing wymyśla
 * kod), a nie baza — dlatego brak {@code @GeneratedValue}. Skutek dla Spring Data: id nigdy nie jest null,
 * więc save uznaje KAŻDY obiekt za istniejący i woła merge → najpierw SELECT, potem INSERT (sekcja 3 lekcji).
 */
@Entity
@Table(name = "discount_codes")
public class DiscountCode {

    @Id
    private String code;

    private int percent;

    protected DiscountCode() {
    }

    public DiscountCode(String code, int percent) {
        this.code = code;
        this.percent = percent;
    }

    public String getCode() {
        return code;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }
}
