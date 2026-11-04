package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Dwa formularze-PUŁAPKI z sekcji 1 i 2: zagnieżdżenie bez {@code @Valid}, a obok to samo poprawnie. */
public final class UncheckedNesting {

    private UncheckedNesting() {
    }

    /** BŁĄD: {@code @NotNull} sprawdza tylko, że adres jest; jego wnętrza (puste miasto) nikt nie sprawdza. */
    public record Forgot(@NotNull Address address, List<OrderLine> lines) {
    }

    /** POPRAWNIE: {@code @Valid} na adresie i na elementach listy. */
    public record Cascade(@NotNull @Valid Address address, List<@NotNull @Valid OrderLine> lines) {
    }
}
