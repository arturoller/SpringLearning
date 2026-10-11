package s05_validation.val04_custom_validators;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Typy do lekcji Val04CustomValidators. */
public final class Val04Types {

    private Val04Types() {
    }

    /** Pozycja z kodem: osobno ProductCode bez i z @NotNull (konwencja null — sekcja 2). */
    public record Item(@ProductCode String optionalCode, @NotNull @ProductCode String requiredCode) {
    }

    /** Kod o nietypowej długości: sześć cyfr. */
    public record LongCodeItem(@ProductCode(digits = 6) String code) {
    }

    /** Promocja: ograniczenie klasy porównuje pola (sekcja 3). Kwoty w groszach. */
    @ValidPromotion
    public record Promotion(LocalDate validFrom, LocalDate validTo, int priceGrosze, int discountGrosze) {
    }

    /** Produkt w katalogu: kod własnym ograniczeniem, kategoria z walidatorem używającym beana (sekcja 4). */
    public record CatalogProduct(@NotNull @ProductCode String code, @ExistingCategory String category) {
    }
}
