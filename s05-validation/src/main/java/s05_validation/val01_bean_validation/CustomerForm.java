package s05_validation.val01_bean_validation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

/**
 * CustomerForm = formularz klienta: tekst, e-mail i dwie daty. Ograniczenia {@code @Email}, {@code @Past}
 * i {@code @Future} nie wymagają wartości — {@code null} uznają za poprawny (patrz PUŁAPKA w lekcji).
 */
public record CustomerForm(
        @NotBlank String name,
        @Email String email,
        @Past LocalDate birthDate,        // past = przeszłość
        @Future LocalDate nextVisit) {    // future = przyszłość
}
