package s21_capstone.cap03_library_with_auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import s21_capstone.cap03_library_with_auth.book.BookController;
import s21_capstone.cap03_library_with_auth.book.BookDtos.BookResponse;
import s21_capstone.cap03_library_with_auth.book.BookService;
import s21_capstone.cap03_library_with_auth.loan.LoanController;
import s21_capstone.cap03_library_with_auth.loan.LoanService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wycinek webowy biblioteki z NASZYMI regułami URL: {@code @Import} konfiguracji Security (łańcuch, dekoder JWT)
 * i zegara. {@code jwt()} ze spring-security-test wkłada gotowe uwierzytelnienie tokenem — bez logowania i bez
 * prawdziwego tokenu (s12_security/Sec06SecurityTesting). Serwisy to atrapy, więc tu sprawdzamy TYLKO reguły URL
 * i HTTP; {@code @PreAuthorize} w serwisach sprawdza Cap03LibraryWithAuthTest (pełny kontekst).
 * UserDetailsService jest atrapą, bo potrzebuje go bean AuthenticationManager z Cap03SecurityConfig.
 */
@WebMvcTest({BookController.class, LoanController.class})
@Import({Cap03SecurityConfig.class, Cap03Config.class})
class Cap03LibraryWithAuthWebSliceTest {

    static final String NEW_BOOK = """
            {"callNumber":"KSI-007","title":"Czysta architektura","author":"Robert C. Martin"}""";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BookService bookService;

    @MockitoBean
    LoanService loanService;

    @MockitoBean
    UserDetailsService userDetailsService;

    static RequestPostProcessor librarian() {
        return jwt().jwt(token -> token.subject("anna")).authorities(new SimpleGrantedAuthority("ROLE_BIBLIOTEKARZ"));
    }

    static RequestPostProcessor reader() {
        return jwt().jwt(token -> token.subject("jan")).authorities(new SimpleGrantedAuthority("ROLE_CZYTELNIK"));
    }

    static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("4. Katalog jest publiczny: GET /api/books bez tokenu → 200")
    void catalogIsPublic() throws Exception {
        when(bookService.all()).thenReturn(List.of(
                new BookResponse(4L, "KSI-004", "Pragmatyczny programista", "Andrew Hunt, David Thomas", true)));
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    @DisplayName("4. POST /api/books: bez tokenu 401 (Bearer), czytelnik 403 (insufficient_scope), bibliotekarz 201")
    void addBookRules() throws Exception {
        mockMvc.perform(json(post("/api/books"), NEW_BOOK))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        org.hamcrest.Matchers.startsWith("Bearer resource_metadata=")));
        mockMvc.perform(json(post("/api/books"), NEW_BOOK).with(reader()))
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate",
                        org.hamcrest.Matchers.containsString("error=\"insufficient_scope\"")));

        when(bookService.add(any())).thenReturn(
                new BookResponse(7L, "KSI-007", "Czysta architektura", "Robert C. Martin", true));
        mockMvc.perform(json(post("/api/books"), NEW_BOOK).with(librarian()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/books/7"));
    }

    @Test
    @DisplayName("4. Wypożyczanie tylko dla bibliotekarza — reguła URL zatrzymuje czytelnika ZANIM ruszy kontroler")
    void lendingIsForLibrarians() throws Exception {
        mockMvc.perform(json(post("/api/loans"), """
                        {"bookId":5,"reader":"adam"}""").with(reader()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/loans/1/return").with(reader()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(loanService);
    }

    @Test
    @DisplayName("2. Konflikt @Version z serwisu (ObjectOptimisticLockingFailureException) → 409 CONCURRENT_MODIFICATION")
    void optimisticLockIs409() throws Exception {
        when(loanService.lend(any())).thenThrow(
                new org.springframework.orm.ObjectOptimisticLockingFailureException(
                        s21_capstone.cap03_library_with_auth.book.Book.class, 6L));
        mockMvc.perform(json(post("/api/loans"), """
                        {"bookId":6,"reader":"adam"}""").with(librarian()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    @DisplayName("1. Walidacja dla zalogowanego bibliotekarza: brak książki i czytelnika → 400 z mapą errors")
    void validation() throws Exception {
        mockMvc.perform(json(post("/api/loans"), "{}").with(librarian()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.bookId").value("książka jest wymagana"))
                .andExpect(jsonPath("$.errors.reader").value("czytelnik jest wymagany"));
    }
}
