package s21_capstone.cap03_library_with_auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import s21_capstone.cap03_library_with_auth.auth.TokenService;
import s21_capstone.cap03_library_with_auth.user.LibraryUserRepository;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy PEŁNEGO KONTEKSTU biblioteki: PRAWDZIWE logowanie (POST /api/auth/login → JWT z bazy użytkowników i BCrypt),
 * prawdziwy dekoder tokenów, reguły URL i {@code @PreAuthorize}/{@code @PostAuthorize} w serwisach. Zegar z profilu
 * cap03 (2026-10-19 10:00). Testy zmieniające dane używają egzemplarzy, których nie czyta żaden inny test (KSI-006).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("cap03")
class Cap03LibraryWithAuthTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TokenService tokenService;

    @Autowired
    Clock clock;

    @Autowired
    LibraryUserRepository users;

    @Autowired
    PasswordEncoder passwordEncoder;

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    static MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    String login(String user) throws Exception {
        String body = mockMvc.perform(json(post("/api/auth/login"), """
                        {"username":"%s","password":"%s-test-123"}""".formatted(user, user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.access_token");
    }

    @Test
    @DisplayName("2. Logowanie: hasło → token JWT z rolami; złe hasło i nieznany login → TEN SAM 401 (nie zdradzamy kont)")
    void loginIssuesJwt() throws Exception {
        String token = login("anna");
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("\"sub\":\"anna\"", "\"roles\":[\"BIBLIOTEKARZ\"]",
                "\"iss\":\"https://biblioteka.example\"");

        for (String body : List.of("""
                {"username":"jan","password":"zle-haslo"}""", """
                {"username":"nikt","password":"zle-haslo"}""")) {
            mockMvc.perform(json(post("/api/auth/login"), body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Nieprawidłowy login lub hasło"))
                    .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
        }
    }

    @Test
    @DisplayName("2. Hasła w bazie: tylko skrót {bcrypt}$2a$10$..., który pasuje do hasła testowego")
    void passwordsAreBcryptHashes() {
        String hash = users.findByUsername("jan").orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("{bcrypt}$2a$10$").doesNotContain("jan-test-123");
        assertThat(passwordEncoder.matches("jan-test-123", hash)).isTrue();
        assertThat(passwordEncoder.matches("jan-test-124", hash)).isFalse();
    }

    @Test
    @DisplayName("4. 401/403/200 na regułach URL: bez tokenu 401, czytelnik 403, bibliotekarz 201; katalog publiczny")
    void urlRules() throws Exception {
        mockMvc.perform(get("/api/books")).andExpect(status().isOk());
        String book = """
                {"callNumber":"KSI-008","title":"Czysta architektura","author":"Robert C. Martin"}""";
        mockMvc.perform(json(post("/api/books"), book))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")));
        mockMvc.perform(as(login("jan"), json(post("/api/books"), book)))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(login("anna"), json(post("/api/books"), book)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("4. @PreAuthorize: czytelnik widzi SWOJE wypożyczenia (200), cudze → 403 ProblemDetail; bibliotekarz — wszystkie")
    void readerSeesOnlyOwnLoans() throws Exception {
        String jan = login("jan");
        mockMvc.perform(as(jan, get("/api/readers/jan/loans")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].daysOverdue").value(4))
                .andExpect(jsonPath("$[1].fine").value(2.00));
        mockMvc.perform(as(jan, get("/api/readers/maria/loans")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(as(login("anna"), get("/api/readers/maria/loans")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].daysOverdue").value(34))
                .andExpect(jsonPath("$[0].fine").value(17.00));
        mockMvc.perform(as(jan, get("/api/me/loans")))
                .andExpect(jsonPath("$[0].reader").value("jan"));
    }

    @Test
    @DisplayName("4. @PostAuthorize: jedno wypożyczenie — własne 200, cudze 403 (wynik sprawdzany PO wykonaniu metody)")
    void singleLoanIsCheckedAfterReading() throws Exception {
        String jan = login("jan");
        mockMvc.perform(as(jan, get("/api/loans/1"))).andExpect(status().isOk());
        mockMvc.perform(as(jan, get("/api/loans/3"))).andExpect(status().isForbidden());
        mockMvc.perform(as(jan, get("/api/loans/999"))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. Wypożyczenie i zwrot z Clock: termin dziś + 14 dni; zwrot w dniu wypożyczenia — kara 0.00; zaległości blokują")
    void lendAndReturn() throws Exception {
        String anna = login("anna");
        mockMvc.perform(as(anna, json(post("/api/loans"), """
                        {"bookId":6,"reader":"maria"}""")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("READER_HAS_OVERDUE"));

        String created = mockMvc.perform(as(anna, json(post("/api/loans"), """
                        {"bookId":6,"reader":"adam"}""")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanedOn").value("2026-10-19"))
                .andExpect(jsonPath("$.dueOn").value("2026-11-02"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Integer loanId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/books/6")).andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(as(anna, json(post("/api/loans"), """
                        {"bookId":6,"reader":"adam"}""")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_ON_LOAN"));
        mockMvc.perform(as(anna, post("/api/loans/" + loanId + "/return")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fine").value(0.00));
        mockMvc.perform(get("/api/books/6")).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("2. Token przeterminowany (wystawiony 2 h temu na 1 h) → 401 i opis w nagłówku WWW-Authenticate")
    void expiredToken() throws Exception {
        String expired = tokenService.issue("jan", List.of("CZYTELNIK"), clock.instant().minus(Duration.ofHours(2)),
                Duration.ofHours(1));
        mockMvc.perform(as(expired, get("/api/me")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        containsString("Jwt expired at 2026-10-19T07:00:00Z")));
        mockMvc.perform(as(login("jan"), get("/api/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorities").value(org.hamcrest.Matchers.contains("FACTOR_BEARER", "ROLE_CZYTELNIK")));
    }
}
