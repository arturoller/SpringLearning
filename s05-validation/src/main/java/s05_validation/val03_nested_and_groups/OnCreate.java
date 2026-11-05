package s05_validation.val03_nested_and_groups;

import jakarta.validation.groups.Default;

/**
 * Grupa walidacji "tworzenie" — interfejs-znacznik (marker interface: pusty interfejs służący tylko jako nazwa).
 * Rozszerza {@link Default}, więc przy walidacji tej grupy sprawdzane są TEŻ wszystkie ograniczenia bez grupy.
 */
public interface OnCreate extends Default {
}
