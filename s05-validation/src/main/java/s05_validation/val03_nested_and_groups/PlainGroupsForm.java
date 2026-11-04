package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;

/**
 * Pułapka grup z sekcji 3: grupa {@link Create} NIE rozszerza {@code Default}. Przy walidacji tej grupy ograniczenie
 * bez grupy ({@code @NotBlank name}) jest POMIJANE.
 *
 * @param id   przy tworzeniu musi być null
 * @param name nazwa — ograniczenie w grupie Default
 */
public record PlainGroupsForm(@Null(groups = Create.class) Long id, @NotBlank String name) {

    /** Grupa-znacznik bez rozszerzania Default. */
    public interface Create {
    }
}
