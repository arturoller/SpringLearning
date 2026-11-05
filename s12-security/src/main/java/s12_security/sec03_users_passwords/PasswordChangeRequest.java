package s12_security.sec03_users_passwords;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Zmiana hasła: stare (żeby ktoś przy cudzym, otwartym komputerze nie zmienił hasła) i nowe. */
public record PasswordChangeRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 12, max = 64, message = "Hasło musi mieć od 12 do 64 znaków") String newPassword) {

    @Override
    public String toString() {
        return "PasswordChangeRequest[oldPassword=[UKRYTE], newPassword=[UKRYTE]]";
    }
}
