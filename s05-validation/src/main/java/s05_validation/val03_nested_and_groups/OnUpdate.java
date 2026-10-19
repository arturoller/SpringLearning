package s05_validation.val03_nested_and_groups;

import jakarta.validation.groups.Default;

/** OnUpdate = grupa walidacji „aktualizacja istniejącego produktu” (też rozszerza Default — patrz OnCreate). */
public interface OnUpdate extends Default {
}
