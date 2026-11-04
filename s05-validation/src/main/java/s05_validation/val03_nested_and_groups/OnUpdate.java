package s05_validation.val03_nested_and_groups;

import jakarta.validation.groups.Default;

/** Grupa walidacji "zmiana" — jak {@link OnCreate}, rozszerza {@link Default}. */
public interface OnUpdate extends Default {
}
