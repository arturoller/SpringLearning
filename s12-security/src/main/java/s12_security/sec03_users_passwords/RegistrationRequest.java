package s12_security.sec03_users_passwords;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dane rejestracji (DTO — obiekt do przenoszenia danych). Walidacja jak w s05_validation/Val02ValidInControllers.
 * Hasło: co najmniej 12 znaków (długość ważniejsza niż "znaki specjalne") i najwyżej 64 — BCrypt bierze pod uwagę tylko
 * 72 BAJTY, a polska litera w UTF-8 zajmuje 2 bajty (sekcja 2).
 */
public record RegistrationRequest(
        @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "[a-z0-9._-]+", message = "Dozwolone: małe litery, cyfry, . _ -")
        String username,
        @NotBlank @Size(min = 12, max = 64, message = "Hasło musi mieć od 12 do 64 znaków")
        String password) {

    /** Hasło nie trafia do logów ani komunikatów — rekord domyślnie wypisałby je w toString() (sekcja 5). */
    @Override
    public String toString() {
        return "RegistrationRequest[username=" + username + ", password=[UKRYTE]]";
    }
}
