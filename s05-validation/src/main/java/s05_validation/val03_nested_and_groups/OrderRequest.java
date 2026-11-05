package s05_validation.val03_nested_and_groups;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.util.List;

/**
 * Zamówienie — jedno DTO dla tworzenia i zmiany, rozróżniane GRUPAMI walidacji ({@link OnCreate}, {@link OnUpdate}).
 * Zawiera obiekt zagnieżdżony ({@code address}), listę obiektów ({@code lines}) i listę tekstów ({@code tags}).
 *
 * @param id              numer zamówienia: przy tworzeniu MUSI być null (nadaje go serwer), przy zmianie MUSI być podany
 * @param address         adres dostawy: wymagany; {@code @Valid} każe sprawdzić też jego wnętrze
 * @param lines           pozycje: lista niepusta, a każdy element sprawdzony ({@code List<@NotNull @Valid OrderLine>}: element nie może być null i jest sprawdzany w głąb)
 * @param tags            etykiety: opcjonalne, ale żadna nie może być pusta ({@code List<@NotBlank String>})
 */
public record OrderRequest(
        @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id,
        @NotNull @Valid Address address,
        @NotEmpty List<@NotNull @Valid OrderLine> lines,
        List<@NotBlank String> tags) {
}
