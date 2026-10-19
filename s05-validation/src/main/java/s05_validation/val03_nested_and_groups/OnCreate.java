package s05_validation.val03_nested_and_groups;

import jakarta.validation.groups.Default;

/**
 * OnCreate = grupa walidacji „tworzenie nowego produktu”. Grupa to zwykły pusty interfejs-znacznik.
 * {@code extends Default} sprawia, że walidacja grupy OnCreate obejmuje TAKŻE ograniczenia bez wskazanej grupy.
 */
public interface OnCreate extends Default {
}
