package s05_validation.val03_nested_and_groups;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Adres dostawy — obiekt ZAGNIEŻDŻONY w zamówieniu. Jego reguły zadziałają tylko wtedy, gdy zamówienie ma
 * {@code @Valid} na polu z adresem.
 *
 * @param street     ulica i numer
 * @param city       miasto
 * @param postalCode kod pocztowy w formacie 00-000
 */
public record Address(
        @NotBlank String street,
        @NotBlank String city,
        @Pattern(regexp = "\\d{2}-\\d{3}") String postalCode) {
}
