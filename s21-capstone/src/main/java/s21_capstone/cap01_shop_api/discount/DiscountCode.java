package s21_capstone.cap01_shop_api.discount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Kod rabatowy (tabela z migracji V3__discount_codes.sql) — infrastruktura pod ĆWICZENIE 1 projektu Cap01ShopApi.
 * Klucz główny to sam kod (napis), bo kod jest naturalnie unikalny i tak go szukamy.
 */
@Entity
@Table(name = "discount_codes")
public class DiscountCode {

    @Id
    @Column(length = 20)
    private String code;

    /** Procent rabatu: 1–90 (pilnuje tego ograniczenie CHECK w bazie). */
    @Column(nullable = false)
    private int percent;

    /** Ostatni dzień ważności (włącznie). */
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    protected DiscountCode() {
    }

    public String getCode() {
        return code;
    }

    public int getPercent() {
        return percent;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }
}
