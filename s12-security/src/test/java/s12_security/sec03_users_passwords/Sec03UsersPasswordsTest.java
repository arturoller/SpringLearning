package s12_security.sec03_users_passwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Base64;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Sec03UsersPasswords — sekcje 1, 5 i 6 (pełny kontekst: ShopUserDetailsService, koder haseł,
 * kontroler). Kodery haseł bez Springa — Sec03UsersPasswordsEncodersTest.
 * {@code @SpringBootTest} + {@code @AutoConfigureMockMvc}: prawdziwy łańcuch filtrów, zapytania bez sieci.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sec03UsersPasswordsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ShopUserDetailsService users;

    @Autowired
    PasswordEncoder passwordEncoder;

    static MockHttpServletRequestBuilder as(String user, String password, MockHttpServletRequestBuilder request) {
        String credentials = user + ":" + password;
        return request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes()));
    }

    @Test
    @DisplayName("1. ShopUserDetailsService tłumaczy konto z „bazy” na UserDetails: skrót hasła, ROLE_ADMIN, blokada")
    void userDetailsServiceLoadsAccounts() {
        UserDetails anna = users.loadUserByUsername("anna");
        assertThat(anna.getUsername()).isEqualTo("anna");
        assertThat(anna.getPassword()).as("w UserDetails jest SKRÓT, nie hasło").startsWith("{bcrypt}$2a$10$");
        assertThat(anna.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(anna.isEnabled()).isTrue();

        assertThat(users.loadUserByUsername("marek").isEnabled()).isFalse();
        assertThatThrownBy(() -> users.loadUserByUsername("nieznany"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Nie ma użytkownika: nieznany");
    }

    @Test
    @DisplayName("1. Logowanie przez HTTP: dobre hasło → 200; złe hasło, nieznany login, zablokowany marek → 401")
    void loginOverHttp() throws Exception {
        mockMvc.perform(as("anna", "anna-test-123", get("/api/me"))).andExpect(status().isOk());
        mockMvc.perform(as("anna", "zle-haslo", get("/api/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as("nieznany", "cokolwiek", get("/api/me"))).andExpect(status().isUnauthorized());
        mockMvc.perform(as("marek", "marek-test-123", get("/api/me"))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("1. DaoAuthenticationProvider: nieznany login udaje „Bad credentials” (jak złe hasło), zablokowany — „User is disabled”")
    void daoAuthenticationProviderHidesUnknownUsers() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);   // to samo składa Spring Security
        provider.setPasswordEncoder(passwordEncoder);
        // komunikaty Security są tłumaczone wg języka — ustalamy angielski, żeby test nie zależał od systemu
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        try {
            assertThat(provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("anna", "anna-test-123"))
                    .isAuthenticated()).isTrue();
            assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("anna", "zle")))
                    .isInstanceOf(BadCredentialsException.class).hasMessage("Bad credentials");
            assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("nieznany", "x")))
                    .isInstanceOf(BadCredentialsException.class).hasMessage("Bad credentials");
            assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("marek", "marek-test-123")))
                    .isInstanceOf(DisabledException.class).hasMessage("User is disabled");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    @DisplayName("1. Komunikaty Security są tłumaczone: po polsku „Niepoprawne dane uwierzytelniające”")
    void messagesAreLocalized() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));
        try {
            assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("anna", "zle")))
                    .hasMessage("Niepoprawne dane uwierzytelniające");
            assertThatThrownBy(() -> provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("marek", "marek-test-123")))
                    .hasMessage("Konto użytkownika jest wyłączone");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    @DisplayName("5. @AuthenticationPrincipal: /api/me zwraca login i uprawnienia (bez hasła)")
    void authenticationPrincipal() throws Exception {
        mockMvc.perform(as("piotr", "piotr-test-123", get("/api/me")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"username\":\"piotr\",\"authorities\":[\"ROLE_USER\"]}"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("5. Authentication po zalogowaniu: UsernamePasswordAuthenticationToken, FACTOR_PASSWORD, hasło usunięte")
    void credentialsAreErasedAfterLogin() throws Exception {
        mockMvc.perform(as("anna", "anna-test-123", get("/api/me/authentication")))
                .andExpect(jsonPath("$.name").value("anna"))
                .andExpect(jsonPath("$.type").value("UsernamePasswordAuthenticationToken"))
                .andExpect(jsonPath("$.authorities[0]").value("FACTOR_PASSWORD"))
                .andExpect(jsonPath("$.authorities[1]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.authorities.length()").value(2))
                .andExpect(jsonPath("$.credentialsIsNull").value(true))
                .andExpect(jsonPath("$.principalPasswordIsNull").value(true));
    }

    @Test
    @DisplayName("5. PUŁAPKA: SecurityContextHolder to zmienna wątku — nowy wątek nie widzi zalogowanego (chyba że Delegating...Runnable)")
    void securityContextIsThreadLocal() throws Exception {
        mockMvc.perform(as("anna", "anna-test-123", get("/api/me/threads")))
                .andExpect(content().json(
                        "{\"sameThread\":\"anna\",\"newThread\":\"nikt\",\"newThreadWithDelegatingRunnable\":\"anna\"}"));
    }

    @Test
    @DisplayName("6. PUŁAPKA: zwrócenie całego UserDetails z „bazy” wysyła klientowi skrót hasła")
    void leakyEndpointExposesHash() throws Exception {
        String json = mockMvc.perform(as("anna", "anna-test-123", get("/api/me/leaky")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json).contains("\"password\":\"{bcrypt}$2a$10$");
    }
}
