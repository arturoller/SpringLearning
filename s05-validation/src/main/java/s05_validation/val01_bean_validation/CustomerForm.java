package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Formularz klienta. Własne komunikaty ({@code message}) są po polsku i NIE zależą od Locale (języka systemu);
 * {@code {regexp}} to wartość atrybutu ograniczenia wstawiona do komunikatu.
 *
 * @param email          adres e-mail: niepusty i o poprawnym kształcie
 * @param phone          telefon: opcjonalny; jeśli jest — 9 do 12 cyfr, z opcjonalnym plusem na początku
 * @param birthDate      data urodzenia: opcjonalna; jeśli jest — z przeszłości
 * @param cardValidUntil data ważności karty stałego klienta: opcjonalna; jeśli jest — z przyszłości
 */
public record CustomerForm(
        @NotBlank @Email String email,
        @Pattern(regexp = "\\+?[0-9]{9,12}", message = "Telefon musi pasować do wzorca {regexp}") String phone,
        @Past LocalDate birthDate,
        @Future LocalDate cardValidUntil) {
}
