package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.Size;

/**
 * Formularz z komunikatem pobieranym z pliku zasobów: {@code {val01.name.short}} w klamrach to KLUCZ, a nie tekst.
 * Treść klucza leży w {@code src/main/resources/val01/Val01Messages.properties} (UTF-8, razem z polskimi literami).
 *
 * @param name nazwa — co najmniej 3 znaki
 */
public record MessageForm(@Size(min = 3, message = "{val01.name.short}") String name) {
}
