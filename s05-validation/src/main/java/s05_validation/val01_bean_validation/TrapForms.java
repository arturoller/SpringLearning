package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Trzy formularze-PUŁAPKI z sekcji 5 lekcji — każdy zawiera typowy błąd początkujących. */
public final class TrapForms {

    private TrapForms() {
    }

    /** Błąd 1: {@code @NotNull} na typie prymitywnym — int NIGDY nie jest null (brak danych daje 0). */
    public record PrimitiveQuantity(@NotNull int quantity) {
    }

    /** Błąd 2: {@code @Size} dotyczy tekstów, kolekcji i tablic — na liczbie nie ma walidatora. */
    public record SizeOnNumber(@Size(min = 1, max = 5) Integer quantity) {
    }

    /** Błąd 3: {@code @Email} przepuszcza null, bo "brak wartości" to nie "zła wartość". */
    public record EmailOnly(@Email String email) {
    }

    /** Poprawnie: wymagalność ({@code @NotNull}) i kształt ({@code @Email}) to dwa osobne ograniczenia. */
    public record EmailRequired(@NotNull @Email String email) {
    }
}
