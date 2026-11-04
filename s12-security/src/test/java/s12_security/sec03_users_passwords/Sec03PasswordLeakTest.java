package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Sec03UsersPasswords: gdzie hash i hasło mogą wyciec. Zwykły test jednostkowy z Jacksonem (Boot 4 = Jackson 3,
 * pakiet {@code tools.jackson}) — dokładnie tym, którym Spring MVC zamienia wynik kontrolera na JSON.
 */
class Sec03PasswordLeakTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("Sekcja 5 (pułapka): encja AppUser zamieniona na JSON zawiera passwordHash — dlatego kontroler zwraca UserView")
    void entityLeaksHash() {
        AppUser entity = new AppUser("anna", "{bcrypt}$2a$10$przykladowyhashtylkodonauki", "CUSTOMER", true);

        assertThat(json.writeValueAsString(entity)).contains("passwordHash", "{bcrypt}");
        assertThat(json.writeValueAsString(UserAccountService.toView(entity)))
                .isEqualTo("{\"username\":\"anna\",\"roles\":[\"CUSTOMER\"]}");
    }

    @Test
    @DisplayName("Sekcja 5: toString() — User Spring Security ukrywa hasło, a nasze rekordy żądań też (nadpisane toString)")
    void toStringHidesPasswords() {
        String user = User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build().toString();
        assertThat(user).contains("Password=[PROTECTED]").doesNotContain("haslo-testowe");
        assertThat(new RegistrationRequest("anna", "haslo-testowe").toString()).doesNotContain("haslo-testowe");
        assertThat(new PasswordChangeRequest("haslo-testowe", "nowe-haslo-testowe").toString()).doesNotContain("haslo");
    }

    @Test
    @DisplayName("Sekcja 5: domyślne toString() rekordu wypisuje WSZYSTKIE pola — także hasło")
    void recordToStringShowsEverything() {
        record NaiveRequest(String username, String password) {
        }
        assertThat(new NaiveRequest("anna", "haslo-testowe").toString()).contains("password=haslo-testowe");
        assertThat(List.of(new NaiveRequest("anna", "haslo-testowe")).toString()).contains("haslo-testowe");
    }
}
