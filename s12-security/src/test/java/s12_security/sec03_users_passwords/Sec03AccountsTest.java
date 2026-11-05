package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Sekcje 3, 4 i 5 lekcji Sec03UsersPasswords przez HTTP: rejestracja, logowanie nowym kontem, konto wyłączone, zmiana hasła,
 * automatyczne przepisanie starego hasha i jednakowe odpowiedzi dla złych danych. {@code @SpringBootTest} +
 * {@code @AutoConfigureMockMvc}, bo potrzebny jest cały łańcuch: kontroler → serwis → JPA → Flyway/H2 → Spring Security.
 * <p>
 * Baza jest wspólna dla wszystkich testów tej klasy (jeden kontekst), więc każdy test, który coś zapisuje, używa WŁASNEJ
 * nazwy użytkownika, a test przepisania hasha sam ustawia stan startowy — kolejność testów nie ma znaczenia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sec03")
class Sec03AccountsTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    AppUserRepository users;

    @Autowired
    JdbcClient jdbc;

    private MvcTestResult register(String username, String password) {
        return mvc.post().uri("/sec03/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}")
                .exchange();
    }

    @Test
    @DisplayName("Sekcja 3: rejestracja → 201 z nazwą i rolami (bez hasha), w bazie {bcrypt}…, a nowe konto od razu loguje się Basic")
    void registerAndLogin() {
        assertThat(register("ola", "dlugie-haslo-testowe"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isStrictlyEqualTo("""
                        {"username": "ola", "roles": ["CUSTOMER"]}
                        """);
        assertThat(users.findByUsername("ola").orElseThrow().getPasswordHash())
                .startsWith("{bcrypt}$2a$10$")
                .doesNotContain("dlugie-haslo-testowe");
        assertThat(mvc.get().uri("/sec03/me").with(httpBasic("ola", "dlugie-haslo-testowe")))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {"username": "ola", "roles": ["CUSTOMER"]}
                        """);
    }

    @Test
    @DisplayName("Sekcja 3: zajęta nazwa → 409 ProblemDetail z kodem USERNAME_TAKEN; za krótkie hasło → 400")
    void duplicateAndInvalid() {
        assertThat(register("anna", "dlugie-haslo-testowe"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("USERNAME_TAKEN");
        assertThat(register("krotki", "krotkie")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(users.existsByUsername("krotki")).isFalse();
    }

    @Test
    @DisplayName("Sekcja 3: konto z migracji (BCrypt) loguje się; /sec03/me bez logowania to 401")
    void seededUser() {
        assertThat(mvc.get().uri("/sec03/me").with(httpBasic("anna", "haslo-testowe"))).hasStatusOk();
        assertThat(mvc.get().uri("/sec03/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Sekcja 4: konto wyłączone (enabled = false) — poprawne hasło i tak daje 401")
    void disabledAccount() {
        assertThat(mvc.get().uri("/sec03/me").with(httpBasic("jan", "haslo-testowe"))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Sekcja 4: zmiana hasła — złe stare hasło 400, poprawne 204; potem stare hasło 401, nowe 200")
    void changePassword() {
        assertThat(register("zmiana", "pierwsze-haslo-testowe")).hasStatus(HttpStatus.CREATED);

        assertThat(mvc.put().uri("/sec03/me/password").with(httpBasic("zmiana", "pierwsze-haslo-testowe"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\": \"nie-to-haslo\", \"newPassword\": \"drugie-haslo-testowe\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("WRONG_PASSWORD");
        assertThat(mvc.put().uri("/sec03/me/password").with(httpBasic("zmiana", "pierwsze-haslo-testowe"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\": \"pierwsze-haslo-testowe\", \"newPassword\": \"drugie-haslo-testowe\"}"))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri("/sec03/me").with(httpBasic("zmiana", "pierwsze-haslo-testowe")))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/sec03/me").with(httpBasic("zmiana", "drugie-haslo-testowe"))).hasStatusOk();
    }

    @Test
    @DisplayName("Sekcja 4: stary hash {noop} po udanym logowaniu sam zamienia się na {bcrypt} (UserDetailsPasswordService)")
    void passwordUpgradeOnLogin() {
        jdbc.sql("UPDATE app_users SET password_hash = '{noop}haslo-testowe' WHERE username = 'stary'").update();
        assertThat(users.findByUsername("stary").orElseThrow().getPasswordHash()).isEqualTo("{noop}haslo-testowe");

        assertThat(mvc.get().uri("/sec03/me").with(httpBasic("stary", "haslo-testowe"))).hasStatusOk();

        assertThat(users.findByUsername("stary").orElseThrow().getPasswordHash()).startsWith("{bcrypt}$2a$10$");
        assertThat(mvc.get().uri("/sec03/me").with(httpBasic("stary", "haslo-testowe"))).hasStatusOk();  // hasło to samo
    }

    @Test
    @DisplayName("Sekcja 5: złe hasło, nieznany login i konto wyłączone dają IDENTYCZNĄ odpowiedź — nie da się wyliczać kont")
    void noUserEnumeration() {
        MvcTestResult wrongPassword = mvc.get().uri("/sec03/me").with(httpBasic("anna", "zle-haslo")).exchange();
        MvcTestResult unknownUser = mvc.get().uri("/sec03/me").with(httpBasic("nikt-taki", "zle-haslo")).exchange();
        MvcTestResult disabled = mvc.get().uri("/sec03/me").with(httpBasic("jan", "zle-haslo")).exchange();

        for (MvcTestResult result : new MvcTestResult[] {wrongPassword, unknownUser, disabled}) {
            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED).hasBodyTextEqualTo("");
            assertThat(result.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE))
                    .isEqualTo(wrongPassword.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE));
        }
    }
}
