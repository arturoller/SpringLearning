package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * ProductDto = produkt używany i przy tworzeniu, i przy aktualizacji. Reguły zależą od sytuacji:
 * id — przy tworzeniu MUSI być puste (nadaje je serwer), przy aktualizacji MUSI być podane. Nazwa i cena — zawsze
 * (to grupa Default, bo adnotacje nie mają {@code groups}). {@code Strict} — grupa bez Default (pułapka, sekcja 4).
 */
public record ProductDto(
        @Null(groups = OnCreate.class, message = "Id nadaje serwer — nie podawaj go przy tworzeniu")
        @NotNull(groups = OnUpdate.class, message = "Id jest wymagane przy aktualizacji")
        Long id,

        @NotBlank(message = "Nazwa jest wymagana")
        String name,

        @NotNull @Positive(message = "Cena musi być dodatnia")
        BigDecimal price,

        @NotBlank(groups = Strict.class, message = "Kategoria jest wymagana w trybie ścisłym")
        String category) {
}
